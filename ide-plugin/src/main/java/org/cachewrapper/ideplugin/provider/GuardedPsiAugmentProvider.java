package org.cachewrapper.ideplugin.provider;

import com.intellij.psi.PsiElement;
import com.intellij.psi.augment.PsiAugmentProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class GuardedPsiAugmentProvider<T extends PsiElement> extends PsiAugmentProvider {

    private final Class<T> supportedType;
    private boolean suppress = false;

    public GuardedPsiAugmentProvider(Class<T> supportedType) {
        this.supportedType = supportedType;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected final <Psi extends PsiElement> List<Psi> getAugments(
            @NotNull PsiElement element,
            @NotNull Class<Psi> type
    ) {
        if (suppress || supportedType != type) {
            return Collections.emptyList();
        }

        try {
            suppress = true;
            return (List<Psi>) doGetAugments(element);
        } finally {
            suppress = false;
        }
    }

    protected abstract List<T> doGetAugments(@NotNull PsiElement element);
}