package org.cachewrapper.ideplugin.provider;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.light.LightMethodBuilder;
import org.cachewrapper.annotation.method.Async;
import org.cachewrapper.ideplugin.util.AnnotationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AsyncMethodAugmentProvider extends GuardedPsiAugmentProvider<PsiMethod> {

    public AsyncMethodAugmentProvider() {
        super(PsiMethod.class);
    }

    @Override
    protected @NotNull List<PsiMethod> doGetAugments(@NotNull PsiElement element) {
        if (!(element instanceof PsiClass psiClass)) return List.of();

        List<PsiMethod> generated = new ArrayList<>();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());

        for (PsiMethod originalMethod : psiClass.getMethods()) {
            if (originalMethod.isConstructor()) continue;

            if (!AnnotationUtils.hasAsync(originalMethod, true)) continue;

            PsiType returnType = originalMethod.getReturnType();
            if (returnType == null) continue;

            @NotNull PsiType completableFutureType =
                    factory.createTypeFromText("java.util.concurrent.CompletableFuture<" +
                            returnType.getCanonicalText() + ">", psiClass);

            LightMethodBuilder asyncMethod = new LightMethodBuilder(
                    psiClass.getManager(),
                    originalMethod.getName() + "Async"
            )
                    .setMethodReturnType(completableFutureType)
                    .setContainingClass(psiClass)
                    .setModifiers(PsiModifier.PUBLIC);

            asyncMethod.setNavigationElement(originalMethod);

            for (PsiParameter param : originalMethod.getParameterList().getParameters()) {
                asyncMethod.addParameter(param.getName(), param.getType());
            }

            asyncMethod.setBaseIcon(originalMethod.getIcon(0));
            asyncMethod.setOriginInfo("Generated async method for @" + Async.class.getSimpleName());

            generated.add(asyncMethod);
        }

        return generated;
    }
}
