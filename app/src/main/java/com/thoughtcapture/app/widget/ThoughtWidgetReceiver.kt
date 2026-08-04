package com.thoughtcapture.app.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ThoughtWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ThoughtWidget()
}
