package edu.sungshin.weatherpick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

class NetworkReceiver(
    private val onNetworkChanged: (Boolean) -> Unit  // 연결 여부 콜백
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)

        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Activity 에게 상태 알려주기
        onNetworkChanged(isConnected)

        // 과제용: 토스트로도 보여주기
        val msg = if (isConnected) "네트워크에 다시 연결되었어요." else "네트워크가 끊겼어요."
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
