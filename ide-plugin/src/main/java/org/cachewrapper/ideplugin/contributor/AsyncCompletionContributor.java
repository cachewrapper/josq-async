package org.cachewrapper.ideplugin.contributor;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class AsyncCompletionContributor extends CompletionContributor {

    public AsyncCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PsiIdentifier.class).inside(PsiClass.class),
                new CompletionProvider<>() {
                    public void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet resultSet
                    ) {
                        PsiElement element = parameters.getPosition();
                        PsiReferenceExpression referenceExpression = PsiTreeUtil.getParentOfType(element, PsiReferenceExpression.class);
                        if (referenceExpression == null) return;

                        PsiExpression qualifier = referenceExpression.getQualifierExpression();
                        if (qualifier == null) return;

                        PsiType type = qualifier.getType();
                        if (!(type instanceof PsiClassType)) return;

                        PsiClass psiClass = ((PsiClassType) type).resolve();
                        if (psiClass == null) return;

                        for (PsiMethod method : psiClass.getMethods()) {
                            String methodName = method.getName();
                            if (methodName.endsWith("Async")) continue;
                            if (method.getModifierList().findAnnotation("org.cachewrapper.annotation.method.Async") == null) return;
                        }
                    }
                }
        );
    }
}