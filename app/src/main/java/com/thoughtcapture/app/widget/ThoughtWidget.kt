package com.thoughtcapture.app.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thoughtcapture.app.MainActivity

class ThoughtWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(androidx.glance.action.actionStartActivity(
                            ComponentName(context, MainActivity::class.java)
                        ))
                        .padding(12)
                ) {
                    Row(
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "✏️",
                            style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE))
                        )
                        Spacer(modifier = GlanceModifier.width(6))
                        Text(
                            text = "快速记录",
                            style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE))
                        )
                    }
                }
            }
        }
    }
}
