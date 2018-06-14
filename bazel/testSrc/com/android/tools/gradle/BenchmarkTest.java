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

package com.android.tools.gradle;

import com.android.testutils.diff.UnifiedDiff;
import com.android.tools.perflogger.Benchmark;
import com.android.tools.perflogger.Metric;
import com.android.tools.perflogger.Metric.MetricSample;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class BenchmarkTest {

    private static final String ROOT = "prebuilts/studio/";

    public static void main(String[] args) throws Exception {

        File distribution = null;
        File repo = null;
        String project = null;
        String benchmarkName = null;
        int warmUps = 0;
        int iterations = 0;
        List<String> tasks = new ArrayList<>();
        List<String> startups = new ArrayList<>();
        List<String> cleanups = new ArrayList<>();
        List<File> mutations = new ArrayList<>();
        List<BenchmarkListener> listeners = new ArrayList<>();

        Iterator<String> it = Arrays.asList(args).iterator();
        while (it.hasNext()) {
            String arg = it.next();
            if (arg.equals("--project") && it.hasNext()) {
                project = it.next();
            } else if (arg.equals("--distribution") && it.hasNext()) {
                distribution = new File(it.next());
            } else if (arg.equals("--repo") && it.hasNext()) {
                repo = new File(it.next());
            } else if (arg.equals("--warmups") && it.hasNext()) {
                warmUps = Integer.valueOf(it.next());
            } else if (arg.equals("--iterations") && it.hasNext()) {
                iterations = Integer.valueOf(it.next());
            } else if (arg.equals("--startup_task") && it.hasNext()) {
                startups.add(it.next());
            } else if (arg.equals("--task") && it.hasNext()) {
                tasks.add(it.next());
            } else if (arg.equals("--cleanup_task") && it.hasNext()) {
                cleanups.add(it.next());
            } else if (arg.equals("--benchmark") && it.hasNext()) {
                benchmarkName = it.next();
            } else if (arg.equals("--mutation") && it.hasNext()) {
                mutations.add(new File(it.next()));
            } else if (arg.equals("--listener") && it.hasNext()) {
                listeners.add(locateListener(it.next()).newInstance());
            } else {
                throw new IllegalArgumentException("Unknown flag: " + arg);
            }
        }

        new BenchmarkTest()
                .run(
                        project,
                        benchmarkName,
                        distribution,
                        repo,
                        warmUps,
                        iterations,
                        mutations,
                        startups,
                        cleanups,
                        tasks,
                        listeners);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends BenchmarkListener> locateListener(String className)
            throws ClassNotFoundException {
        String fqcn =
                className.indexOf('.') != -1
                        ? className
                        : BenchmarkTest.class.getPackage().getName() + "." + className;

        return (Class<? extends BenchmarkListener>)
                BenchmarkTest.class.getClassLoader().loadClass(fqcn);
    }

    private static String getLocalGradleVersion() throws IOException {
        try (FileInputStream fis = new FileInputStream("tools/buildSrc/base/version.properties")) {
            Properties properties = new Properties();
            properties.load(fis);
            return properties.getProperty("buildVersion");
        }
    }

    public void run(
            String project,
            String benchmarkName,
            File distribution,
            File repo,
            int warmUps,
            int iterations,
            List<File> mutations,
            List<String> startups,
            List<String> cleanups,
            List<String> tasks,
            List<BenchmarkListener> listeners)
            throws Exception {

        Benchmark benchmark =
                new Benchmark.Builder(benchmarkName).setProject("Android Studio Gradle").build();
        File data = new File(ROOT + "buildbenchmarks/" + project);
        File out = new File(System.getenv("TEST_TMPDIR"), ".gradle_out");
        File src = new File(System.getenv("TEST_TMPDIR"), ".gradle_src");
        File home = new File(System.getenv("TEST_TMPDIR"), ".home");
        home.mkdirs();

        Gradle.unzip(new File(data, "src.zip"), src);
        UnifiedDiff diff = new UnifiedDiff(new File(data, "setup.diff"));
        diff.apply(src, 3);

        UnifiedDiff[] diffs = new UnifiedDiff[mutations.size()];
        for (int i = 0; i < mutations.size(); i++) {
            diffs[i] = new UnifiedDiff(mutations.get(i));
        }

        try (Gradle gradle = new Gradle(src, out, distribution)) {
            gradle.addRepo(repo);
            gradle.addRepo(new File(data, "repo.zip"));
            gradle.addArgument("-Dcom.android.gradle.version=" + getLocalGradleVersion());
            gradle.addArgument("-Duser.home=" + home.getAbsolutePath());
            listeners.forEach(it -> it.configure(home, gradle));

            gradle.run(startups);

            Metric totalBuildTime = new Metric("TOTAL_BUILD_TIME");
            listeners.forEach(it -> it.benchmarkStarting(benchmark));
            for (int i = 0; i < warmUps + iterations; i++) {
                gradle.run(cleanups);

                for (int j = 0; j < diffs.length; j++) {
                    diffs[j].apply(src, 3);
                    diffs[j] = diffs[j].invert();
                }
                if (i >= warmUps) {
                    listeners.forEach(it -> it.iterationStarting());
                }
                long start = System.currentTimeMillis();
                gradle.run(tasks);
                if (i >= warmUps) {
                    totalBuildTime.addSamples(
                            benchmark, new MetricSample(start, System.currentTimeMillis() - start));
                    listeners.forEach(BenchmarkListener::iterationDone);
                }
            }
            listeners.forEach(BenchmarkListener::benchmarkDone);
            totalBuildTime.commit();
        }
    }
}
