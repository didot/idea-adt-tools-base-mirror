/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tools.lint.psi;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;

class EcjPsiExpressionStatement extends EcjPsiStatement implements PsiExpressionStatement {

    private PsiExpression mExpression;

    EcjPsiExpressionStatement(@NonNull EcjPsiManager manager,
            @Nullable Statement statement) {
        super(manager, statement);
    }

    void setExpression(PsiExpression expression) {
        mExpression = expression;
    }

    @NonNull
    @Override
    public PsiExpression getExpression() {
        return mExpression;
    }

    @Override
    public void accept(@NonNull PsiElementVisitor visitor) {
        if (visitor instanceof JavaElementVisitor) {
            ((JavaElementVisitor) visitor).visitExpressionStatement(this);
        } else {
            visitor.visitElement(this);
        }
    }
}
