/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.CLASS_VIEWGROUP;
import static com.android.SdkConstants.DOT_LAYOUT_PARAMS;
import static com.android.SdkConstants.R_CLASS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collections;
import java.util.List;

/**
 * Makes sure that custom views use a declare styleable that matches
 * the name of the custom view
 */
public class CustomViewDetector extends Detector implements Detector.JavaPsiScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            CustomViewDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Mismatched style and class names */
    public static final Issue ISSUE = Issue.create(
            "CustomViewStyleable",
            "Mismatched Styleable/Custom View Name",

            "The convention for custom views is to use a `declare-styleable` whose name " +
            "matches the custom view class name. The IDE relies on this convention such that " +
            "for example code completion can be offered for attributes in a custom view " +
            "in layout XML resource files.\n" +
            "\n" +
            "(Similarly, layout parameter classes should use the suffix `_Layout`.)",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    private static final String OBTAIN_STYLED_ATTRIBUTES = "obtainStyledAttributes";

    /** Constructs a new {@link CustomViewDetector} check */
    public CustomViewDetector() {
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(OBTAIN_STYLED_ATTRIBUTES);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable JavaElementVisitor visitor,
            @NonNull PsiMethodCallExpression node, @NonNull PsiMethod method) {
        if (!context.getEvaluator().isMemberInSubClassOf(method, CLASS_CONTEXT, false)) {
            return;
        }
        PsiExpression[] arguments = node.getArgumentList().getExpressions();
        int size = arguments.length;
        // Which parameter contains the styleable (attrs) ?
        int parameterIndex;
        if (size == 1) {
            // obtainStyledAttributes(int[] attrs)
            parameterIndex = 0;
        } else {
            // obtainStyledAttributes(int resid, int[] attrs)
            // obtainStyledAttributes(AttributeSet set, int[] attrs)
            // obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes)
            parameterIndex = 1;
        }
        PsiExpression expression = arguments[parameterIndex];
        if (!(expression instanceof PsiReferenceExpression)) {
            return;
        }
        PsiReferenceExpression nameRef = (PsiReferenceExpression)expression;
        PsiExpression styleableQualifier = nameRef.getQualifierExpression();
        if (!(styleableQualifier instanceof PsiReferenceExpression)) {
            return;
        }
        PsiReferenceExpression styleableRef = (PsiReferenceExpression)styleableQualifier;
        if (!ResourceType.STYLEABLE.getName().equals(styleableRef.getReferenceName())) {
            return;
        }
        PsiExpression rQualifier = styleableRef.getQualifierExpression();
        if (!(rQualifier instanceof PsiReferenceExpression)) {
            return;
        }
        PsiReferenceExpression rReference = (PsiReferenceExpression)rQualifier;
        if (rReference.getQualifierExpression() != null
                || !R_CLASS.equals(rReference.getReferenceName())) {
            return;
        }

        String styleableName = nameRef.getReferenceName();
        if (styleableName == null) {
            return;
        }

        PsiClass cls = PsiTreeUtil.getParentOfType(node, PsiClass.class, false);
        if (cls == null) {
            return;
        }

        String className = cls.getName();
        if (context.getEvaluator().extendsClass(cls, CLASS_VIEW, false)) {
            if (!styleableName.equals(className)) {
                String message = String.format(
                        "By convention, the custom view (`%1$s`) and the declare-styleable (`%2$s`) "
                                + "should have the same name (various editor features rely on "
                                + "this convention)",
                        className, styleableName);
                context.report(ISSUE, node, context.getLocation(expression), message);
            }
        } else if (context.getEvaluator().extendsClass(cls,
                CLASS_VIEWGROUP + DOT_LAYOUT_PARAMS, false)) {
            PsiClass outer = PsiTreeUtil.getParentOfType(cls, PsiClass.class, true);
            if (outer == null) {
                return;
            }
            String layoutClassName = outer.getName();
            String expectedName = layoutClassName + "_Layout";
            if (!styleableName.equals(expectedName)) {
                String message = String.format(
                        "By convention, the declare-styleable (`%1$s`) for a layout parameter "
                                + "class (`%2$s`) is expected to be the surrounding "
                                + "class (`%3$s`) plus \"`_Layout`\", e.g. `%4$s`. "
                                + "(Various editor features rely on this convention.)",
                        styleableName, className, layoutClassName, expectedName);
                context.report(ISSUE, node, context.getLocation(expression), message);
            }
        }
    }
}
