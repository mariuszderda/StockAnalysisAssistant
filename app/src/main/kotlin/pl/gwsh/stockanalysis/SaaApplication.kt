package pl.gwsh.stockanalysis

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punkt wejścia aplikacji. Adnotacja Hilta generuje komponent DI dla całego
 * cyklu życia procesu (SingletonComponent). Wszystkie moduły z pakietu
 * pl.gwsh.stockanalysis.di są instalowane właśnie tutaj.
 */
@HiltAndroidApp
class SaaApplication : Application()
