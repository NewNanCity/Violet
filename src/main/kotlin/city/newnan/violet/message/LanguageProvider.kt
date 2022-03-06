package city.newnan.violet.message

interface LanguageProvider {
    fun provideLanguage(rawText: String): String?
}
