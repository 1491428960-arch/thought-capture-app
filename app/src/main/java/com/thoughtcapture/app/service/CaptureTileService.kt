package com.thoughtcapture.app.service

import android.content.Intent
import android.service.quicksettings.TileService
import com.thoughtcapture.app.MainActivity

class CaptureTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("open_capture", true)
        }
        startActivityAndCollapse(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state = android.service.quicksettings.Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}
