package com.doublesymmetry.kotlinaudio.notification

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class CustomExoPlayerNotificationManager(
    context: Context,
    channelId: String,
    notificationId: Int,
    mediaDescriptionAdapter: MediaDescriptionAdapter,
    notificationListener: NotificationListener?,
    customActionReceiver: CustomActionReceiver?,
    smallIconResourceId: Int,
    playActionIconResourceId: Int,
    pauseActionIconResourceId: Int,
    stopActionIconResourceId: Int,
    rewindActionIconResourceId: Int,
    fastForwardActionIconResourceId: Int,
    previousActionIconResourceId: Int,
    nextActionIconResourceId: Int,
    groupKey: String?
) : PlayerNotificationManager(context, channelId, notificationId, mediaDescriptionAdapter, notificationListener, customActionReceiver, smallIconResourceId, playActionIconResourceId, pauseActionIconResourceId, stopActionIconResourceId, rewindActionIconResourceId, fastForwardActionIconResourceId, previousActionIconResourceId, nextActionIconResourceId, groupKey) {
    override fun getActions(player: Player): MutableList<String> {
//        return super.getActions(player)
        return emptyList<String>().toMutableList()
    }

    override fun getActionIndicesForCompactView(actionNames: MutableList<String>, player: Player): IntArray {
        return super.getActionIndicesForCompactView(actionNames, player)
    }

    //
    public class Builder(context: Context, notificationId: Int, channelId: String) : PlayerNotificationManager.Builder(context, notificationId, channelId) {
        override fun build(): CustomExoPlayerNotificationManager {
            super.build()
            return CustomExoPlayerNotificationManager(
                context,
                channelId,
                notificationId,
                mediaDescriptionAdapter,
                notificationListener,
                customActionReceiver,
                smallIconResourceId,
                playActionIconResourceId,
                pauseActionIconResourceId,
                stopActionIconResourceId,
                rewindActionIconResourceId,
                fastForwardActionIconResourceId,
                previousActionIconResourceId,
                nextActionIconResourceId,
                groupKey
            )
        }
    }
}