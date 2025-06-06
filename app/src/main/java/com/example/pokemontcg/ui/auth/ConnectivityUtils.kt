package com.example.pokemontcg.ui.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Comprueba si hay conectividad a Internet
object ConnectivityUtils {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.activeNetwork ?: return false
        val nc = cm.getNetworkCapabilities(cap) ?: return false
        return nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}