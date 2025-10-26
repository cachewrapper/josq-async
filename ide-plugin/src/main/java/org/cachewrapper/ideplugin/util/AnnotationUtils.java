package org.cachewrapper.ideplugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.cachewrapper.annotation.method.Async;
import org.eclipse.sisu.Nullable;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class AnnotationUtils {

    public static boolean hasAsync(
            @Nullable PsiMethod psiMethod,
            boolean includeClass
    ) {
        return psiMethod != null && (hasAsync(psiMethod) || (includeClass && hasAsync(psiMethod.getContainingClass())));
    }

    public static boolean hasAsync(
            @Nullable PsiModifierListOwner owner
    ) {
        return owner != null && isAnnotationPresent(owner.getModifierList(), Async.class);
    }

    public static Optional<PsiAnnotation> findAsync(
            @Nullable PsiModifierListOwner owner
    ) {
        if (owner != null && owner.getModifierList() != null) {
            return Optional.ofNullable(owner.getModifierList().findAnnotation(Async.class.getName()));
        }

        return Optional.empty();
    }

    @SafeVarargs
    public static boolean isAnnotationPresent(
            @Nullable PsiModifierList psiModifierList,
            @NotNull Class<? extends Annotation>... annotationClasses
    ) {
        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            if (psiModifierList != null && psiModifierList.findAnnotation(annotationClass.getName()) != null) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static PsiAnnotation createAnnotation(
            @NotNull Project project,
            @NotNull Class<? extends Annotation> annotationClass
    ) {
        return JavaPsiFacade.getElementFactory(project)
                .createAnnotationFromText("@" + annotationClass.getName(), null);
    }
}