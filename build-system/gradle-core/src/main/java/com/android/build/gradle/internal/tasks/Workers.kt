/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.internal.tasks

import com.android.build.gradle.internal.profile.ProfilerInitializer
import com.android.build.gradle.internal.profile.TaskProfilingRecord
import com.android.build.gradle.options.BooleanOption
import com.android.build.gradle.options.ProjectOptions
import com.android.ide.common.workers.ExecutorServiceAdapter
import com.android.ide.common.workers.WorkerExecutorException
import com.android.ide.common.workers.WorkerExecutorFacade
import com.google.common.annotations.VisibleForTesting
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutionException
import org.gradle.workers.WorkerExecutor
import java.io.Serializable
import java.lang.reflect.Constructor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import javax.inject.Inject

/**
 * Singleton object responsible for providing instances of [WorkerExecutorFacade]
 * in the context of the current build settings (like whether or not we should use
 * Gradle's [WorkerExecutor] or the level of parallelism allowed by users.
 */
object Workers {

    /**
     * A flag to force using a [DirectWorkerExecutor] instead of Gradle's executor.
     *
     * This should be used only in unit tests testing code that uses [WorkerExecutorFacade]. This
     * should be set to true only before the test and reset back to false after the test to not
     * affect other tests.
     */
    @VisibleForTesting
    var useDirectWorkerExecutor: Boolean = false

    /**
     * Factory function for creating instances of [WorkerExecutorFacade].
     * Initialized with a default version using the the [ForkJoinPool.commonPool]
     */
    private var factory: (owner: String, worker: WorkerExecutor, executor: ExecutorService?) -> WorkerExecutorFacade =
        { _, _, executor -> ExecutorServiceAdapter(executor ?: ForkJoinPool.commonPool()) }

    /**
     * Creates a [WorkerExecutorFacade] using the passed [WorkerExecutor], delegating
     * to the [factory] method for the actual instantiation of the interface.
     *
     * @param owner the task path issuing the request and owning the [WorkerExecutor] instance.
     * @param worker [WorkerExecutor] to use if Gradle's worker executor are enabled.
     * @param executor [ExecutorService] to use if the Gradle's worker are not enabled or null
     * if the default installed version is to be used.
     * @return an instance of [WorkerExecutorFacade] using the passed worker or the default
     * [ExecutorService] depending on the project options.
     */
    @JvmOverloads
    fun getWorker(owner: String, worker: WorkerExecutor, executor: ExecutorService? = null)
            : WorkerExecutorFacade {
        return if (useDirectWorkerExecutor) {
            DirectWorkerExecutor()
        } else factory(owner, worker, executor)
    }

    /**
     * factory function initializer that uses the project's [ProjectOptions] to decide which
     * instance of [WorkerExecutorFacade] should be used. This function should be registered as the
     * [factory] method early during our plugin instantiation.
     *
     * if [useDirectWorkerExecutor] is enabled, will use [DirectWorkerExecutor] otherwise
     * will use [BooleanOption.ENABLE_GRADLE_WORKERS] to determine if [WorkerExecutor] or
     * [ExecutorService] should be used.
     *
     * @param options Gradle's project options.
     * @param defaultExecutor default [ExecutorService] to use when none is explicitly provided when
     * invoking [getWorker] API.
     */
    fun initFromProject(options: ProjectOptions, defaultExecutor: ExecutorService) {
        factory = when {
            options.get(BooleanOption.ENABLE_GRADLE_WORKERS) -> {
                { owner, worker, _ -> WorkerExecutorAdapter(owner, worker) }
            }
            else -> {
                { _, _, executor -> ExecutorServiceAdapter(executor ?: defaultExecutor) }
            }
        }
    }

    /** An implementation of [WorkerExecutorFacade] that executes runnables directly */
    private class DirectWorkerExecutor : WorkerExecutorFacade {

        override fun submit(
            actionClass: Class<out Runnable>,
            parameter: Serializable
        ) {
            val constructor = actionClass.getDeclaredConstructor(parameter.javaClass)
            val isConstructorAccessible = constructor.isAccessible
            constructor.isAccessible = true
            val action = constructor.newInstance(parameter)
            action.run()
            constructor.isAccessible = isConstructorAccessible
        }

        override fun await() {
            // do nothing.
        }

        override fun close() {
            // do nothing.
        }
    }

    /**
     * Simple implementation of [WorkerExecutorFacade] that uses a Gradle [WorkerExecutor]
     * to submit new work actions.
     *
     */
    private class WorkerExecutorAdapter(
        private val owner: String,
        private val workerExecutor: WorkerExecutor
    ) :
        WorkerExecutorFacade {

        val taskRecord by lazy {
            (ProfilerInitializer.getListener()?.getTaskRecord(owner)
                ?: TaskProfilingRecord.dummyTaskRecord)
        }

        override fun submit(
            actionClass: Class<out Runnable>,
            parameter: Serializable
        ) {

            val workerKey = "$owner${actionClass.name}${parameter.hashCode()}"
            val submissionParameters = ActionParameters(
                actionClass,
                parameter,
                owner,
                workerKey
            )

            taskRecord.addWorker(workerKey)

            workerExecutor.submit(ActionFacade::class.java) {
                it.isolationMode = IsolationMode.NONE
                it.params(submissionParameters)
            }
        }

        override fun await() {
            try {
                taskRecord.setTaskWaiting()
                workerExecutor.await()
            } catch (e: WorkerExecutionException) {
                throw WorkerExecutorException(e.causes)
            }
        }

        /**
         * In a normal situation you would like to call await() here, however:
         * 1) Gradle currently can only run a SINGLE @TaskAction for a given project
         *    (and this should be fixed!)
         * 2) WorkerExecutor passed to a task instance is tied to the task and Gradle is able
         *    to control which worker items are executed by which task
         *
         * Thus, if you put await() here, only a single task can run.
         * If not (as it is), gradle will start another task right after it finishes executing a
         * @TaskAction (which ideally should be just some preparation + a number of submit() calls
         * to a WorkerExecutorFacade. In case the task B depends on the task A and the work items
         * of the task A hasn't finished yet, gradle will call await() on the dedicated
         * WorkerExecutor of the task A and therefore work items will finish before task B
         * @TaskAction starts (so, we are safe!).
         */
        override fun close() {
            taskRecord.setTaskClosed()
        }
    }

    class ActionParameters(
        val delegateAction: Class<out Runnable>,
        val delegateParameters: Serializable,
        val taskOwner: String,
        private val workerKey: String
    ) : Serializable {

        private fun taskRecord() =
            (ProfilerInitializer.getListener()?.getTaskRecord(taskOwner)
                ?: TaskProfilingRecord.dummyTaskRecord)

        fun getWorker() = taskRecord().get(workerKey)
    }

    class ActionFacade @Inject constructor(val params: ActionParameters) : Runnable {

        override fun run() {
            val constructor = findAppropriateConstructor()
                ?: throw RuntimeException("Cannot find constructor with @Inject in ${params.delegateAction.name}")

            val delegate = constructor.newInstance(params.delegateParameters) as Runnable
            params.getWorker().executionStarted()
            delegate.run()
            params.getWorker().executionFinished()
        }

        private fun findAppropriateConstructor(): Constructor<*>? {
            for (constructor in params.delegateAction.constructors) {
                if (constructor.parameterTypes.size == 1
                    && constructor.isAnnotationPresent(Inject::class.java)
                    && Serializable::class.java.isAssignableFrom(constructor.parameterTypes[0])
                ) {
                    constructor.isAccessible = true
                    return constructor
                }
            }
            return null
        }


    }
}