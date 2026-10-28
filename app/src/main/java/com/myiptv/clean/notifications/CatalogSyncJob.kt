package com.myiptv.clean.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.myiptv.clean.data.repository.MediaPlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object CatalogSyncScheduler {
    private const val JOB_ID = 0x53414D

    /** Schedule once from the activity; never reschedule the job from its own service process. */
    fun schedule(context: Context) {
        try {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            if (scheduler.allPendingJobs.any { it.id == JOB_ID }) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, CatalogSyncJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(MediaPlaylistRepository.REFRESH_INTERVAL_MS)
                .setPersisted(true)
                .build()
            if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) {
                Log.w("SamPlay", "No se pudo programar la búsqueda de novedades en segundo plano")
            }
        } catch (error: RuntimeException) {
            // Foreground refresh remains available even if a device rejects scheduled jobs.
            Log.w("SamPlay", "No se pudo programar la búsqueda de novedades", error)
        }
    }
}

/** Android can postpone periodic work for battery, connectivity, standby, or a force stop. */
class CatalogSyncJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = scope.launch {
            val success = try {
                MediaPlaylistRepository.refresh(applicationContext)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("SamPlay", "Falló la búsqueda de novedades", error)
                false
            }
            if (isActive) jobFinished(params, !success)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        running = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
