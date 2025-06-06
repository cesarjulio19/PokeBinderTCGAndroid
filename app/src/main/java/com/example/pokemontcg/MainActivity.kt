package com.example.pokemontcg

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.NavGraph
import com.example.pokemontcg.ui.LocaleHelper

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var prefs: SharedPreferences
    private val navController by lazy { findNavController(R.id.nav_host_fragment) }

    override fun attachBaseContext(newBase: Context) {
        // Obtenemos SharedPreferences de forma manual; así no dependemos de Hilt en este punto
        val preferences = newBase.getSharedPreferences("prefs_pokemontcg", Context.MODE_PRIVATE)
        val savedLang = preferences.getString("app_language", "es") ?: "es"
        val contextLocalized = LocaleHelper.updateAppLocale(newBase, savedLang)
        super.attachBaseContext(contextLocalized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //  Ajustamos el grafo de navegación en tiempo de creación:
        //    Si ya hay token, arrancamos en cartas; si no, en login.
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        val estaLogueado = prefs.getString("jwt", null) != null
         if (estaLogueado) {
            navGraph.setStartDestination(R.id.cardsFragment)
        } else {
             navGraph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = navGraph

        // Configuramos la Toolbar para que funcione con el NavController
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val appBarConfig = AppBarConfiguration(navGraph)
        setupActionBarWithNavController(navController, appBarConfig)
    }

    // Inflamos el menú correspondiente según el estado de sesión
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        val estaLogueado = prefs.getString("jwt", null) != null

        if (estaLogueado) {
            // Si hay JWT, inflamos el menú para usuarios logueados
            menuInflater.inflate(R.menu.menu_logged, menu)
        } else {
            // Si no hay JWT, inflamos el menú para usuarios no logueados
            menuInflater.inflate(R.menu.menu_not_logged, menu)
        }
        return true
    }

    //  Cada vez que se invaliden las opciones (invalidateOptionsMenu),
    //    onCreateOptionsMenu volverá a ejecutarse y elegirá el menú adecuado.
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // (Podríamos hacer lógica adicional aquí si quisiéramos,
        //  pero con onCreateOptionsMenu suficiente.)
        return super.onPrepareOptionsMenu(menu)
    }

    //Procesamos los clicks sobre los ítems del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Si el usuario pulsa “Idiomas” en el menú_logged_in
            R.id.action_switch_language -> {
                showLanguageDialog()
                true
            }
            // Si el usuario pulsa “Cartas” en el menú_logged_in
            R.id.action_ir_cartas -> {
                navController.navigate(R.id.cardsFragment)
                true
            }
            // Si el usuario pulsa “Cerrar sesión” en menu_logged_in
            R.id.action_cerrar_sesion -> {
                // 1) Limpiamos SharedPreferences (borramos JWT)
                prefs.edit().clear().apply()
                // 2) Forzamos que el menú se vuelva a inflar en estado logged-out
                invalidateOptionsMenu()
                // 3) Navegamos a LoginFragment (y vaciamos BackStack si hace falta)
                navController.navigate(R.id.loginFragment)
                true
            }
            // Si el usuario pulsa “Iniciar Sesión” en menu_logged_out
            R.id.action_iniciar_sesion -> {
                navController.navigate(R.id.loginFragment)
                true
            }
            // Si el usuario pulsa “Registrarse” en menu_logged_out
            R.id.action_registrarse -> {
                navController.navigate(R.id.registerFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun showLanguageDialog() {
        val options = arrayOf("Español", "English")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_change_language))
            .setSingleChoiceItems(options, getSavedLanguageIndex()) { dialog, which ->
                val newLocale = if (which == 0) "es" else "en"
                prefs.edit().putString("app_language", newLocale).apply()
                recreate() // recrea la Activity para forzar el cambio de idioma
                dialog.dismiss()
            }
            .show()
    }

    private fun updateAppLocale(language: String) {
        // Actualiza el contexto de toda la aplicación:
        val newContext = LocaleHelper.updateAppLocale(this, language)
        //  Recrea la Activity para que vuelva a inflar con los nuevos strings:
        recreate()
    }


    private fun getSavedLanguageIndex(): Int {
        val lang = prefs.getString("app_language", "es") ?: "es"
        return if (lang == "en") 1 else 0
    }
    private fun saveSelectedLanguage(localeCode: String) {
        prefs.edit().putString("app_language", localeCode).apply()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}
