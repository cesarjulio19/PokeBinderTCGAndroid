package com.example.pokemontcg

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pokemontcg.ui.card.CardRepository
import com.example.pokemontcg.ui.set.SetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncStrapiWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val setRepo: SetRepository,
    private val cardRepo: CardRepository,
) : CoroutineWorker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "sync_channel"
        const val NOTIF_ID   = 42
    }

    override suspend fun doWork(): Result {
        return try {
            // Refresca todos los sets
            setRepo.refreshSetsFromApi()

            // Obtiene los IDs de set una sola vez
            val setIds = setRepo.getAllSetIdsOnce()

            // Para cada set, sincroniza sus cartas
            setIds.forEach { cardRepo.syncCardsBySet(it) }

            // Al finalizar con éxito, lanza la notificación
            sendNotification(setIds.size)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(updatedSetsCount: Int) {
        // Construye la notificación
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.tarjetas)                   // tu icono
            .setContentTitle("Sincronización completada")
            .setContentText("Se han actualizado $updatedSetsCount sets y sus cartas.")
            .setAutoCancel(true)
            .build()

        // Comprueba el permiso antes de notificar
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIF_ID, notif)
        }
    }
}
