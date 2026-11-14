package com.myiptv.clean.ui.compose

import android.content.Context
import android.util.Log

internal class PreferencesFavoritesStore(context: Context) : FavoritesStore {
    private val preferences = context.applicationContext.getSharedPreferences("samplay_favorites", Context.MODE_PRIVATE)

    override fun read(): Set<String> = try {
        preferences.getStringSet("ids", emptySet()).orEmpty().toSet()
    } catch (error: RuntimeException) {
        Log.w("SamPlay", "No se pudieron leer los favoritos guardados", error)
        emptySet()
    }

    override fun write(ids: Set<String>) {
        try {
            // SharedPreferences string sets must not be mutated after they are passed in.
            preferences.edit().putStringSet("ids", ids.toSet()).apply()
        } catch (error: RuntimeException) {
            // Keep the in-memory session usable if local storage fails.
            Log.w("SamPlay", "No se pudieron guardar los favoritos", error)
        }
    }
}
