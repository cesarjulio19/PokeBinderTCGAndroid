package com.example.pokemontcg

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pokemontcg.ui.LocaleHelper
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1001
        private const val CHANNEL_ID = "sync_channel"
        private const val CHANNEL_NAME = "Sincronización"

    }

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

        // Crea canal de notificaciones (solo una vez)
        createNotificationChannel()

        // Pedir permiso NOTIFICATIONS
        ensureNotificationPermission()

        // Programar Worker periódico cada hora
        scheduleSyncWorker()
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
            // Si el usuario pulsa "Idiomas" en el menú_logged_in
            R.id.action_switch_language -> {
                showLanguageDialog()
                true
            }
            // Si el usuario pulsa "Cartas" en el menú_logged_in
            R.id.action_ir_cartas -> {
                navController.navigate(R.id.cardsFragment)
                true
            }
            // Si el usuario pulsa "Abrir Sobres" en el menú_logged_in
            R.id.action_open_pack -> {
                navController.navigate(R.id.openPackFragment)
                true
            }

            // Si el usuario pulsa "Abrir Sobres" en el menú_logged_in
            R.id.action_profile -> {
                navController.navigate(R.id.profileFragment)
                true
            }
            // Si el usuario pulsa "Cerrar Sesión" en menu_logged_in
            R.id.action_cerrar_sesion -> {
                // Limpiamos SharedPreferences (borramos JWT)
                prefs.edit().clear().apply()
                // Forzamos que el menú se vuelva a inflar en estado logged-out
                invalidateOptionsMenu()
                // Navegamos a LoginFragment (y vaciamos BackStack si hace falta)
                navController.navigate(R.id.loginFragment)
                true
            }
            // Si el usuario pulsa "Iniciar Sesión" en menu_logged_out
            R.id.action_iniciar_sesion -> {
                navController.navigate(R.id.loginFragment)
                true
            }
            // Si el usuario pulsa "Registrarse" en menu_logged_out
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



    private fun getSavedLanguageIndex(): Int {
        val lang = prefs.getString("app_language", "es") ?: "es"
        return if (lang == "en") 1 else 0
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para avisar cuando se complete la sincronización"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun scheduleSyncWorker() {
        // Único y cada hora
        val workRequest = PeriodicWorkRequestBuilder<SyncStrapiWorker>(
            30, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "sync_strapi",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }


}
