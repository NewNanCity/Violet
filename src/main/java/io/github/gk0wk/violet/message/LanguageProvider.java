package io.github.gk0wk.violet.message;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public interface LanguageProvider {
    @Nonnull
    String provideLanguage(@NotNull String rawText);
}
