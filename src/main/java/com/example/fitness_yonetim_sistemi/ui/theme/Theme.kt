package com.example.fitness_yonetim_sistemi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// TUF GYM için özelleştirilmiş tema
private val TufGymColorScheme = lightColorScheme(
    primary = MaviAna,
    secondary = MaviKoyu,
    tertiary = MaviAcik,
    background = ArkaPlanAcik,
    surface = YuzeyBeyaz,
    onPrimary = YuzeyBeyaz,
    onSecondary = YuzeyBeyaz,
    onBackground = MetinKoyu,
    onSurface = MetinKoyu
)

@Composable
fun TufGymTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TufGymColorScheme,
        typography = Typography,
        content = content
    )
}