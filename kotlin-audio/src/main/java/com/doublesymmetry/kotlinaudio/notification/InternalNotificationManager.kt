package com.doublesymmetry.kotlinaudio.notification

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil

class InternalNotificationManager(
    context: Context,
    channelId: String,
    notificationId: Int,
    mediaDescriptionAdapter: MediaDescriptionAdapter,
    notificationListener: NotificationListener?,
    primaryActionReceiver: PrimaryActionReceiver?,
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
) : PlayerNotificationManager(
    context,
    channelId,
    notificationId,
    mediaDescriptionAdapter,
    notificationListener,
    primaryActionReceiver,
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
) {
    override fun getActions(player: Player): MutableList<String> {
        val stringActions: MutableList<String> = ArrayList()
        if (usePreviousAction) {
            stringActions.add(ACTION_PREVIOUS)
        }
        if (useRewindAction) {
            stringActions.add(ACTION_REWIND)
        }
        if (usePlayPauseActions) {
            if (shouldShowPauseButton(player)) {
                stringActions.add(ACTION_PAUSE)
            } else {
                stringActions.add(ACTION_PLAY)
            }
        }
        if (useStopAction) {
            stringActions.add(ACTION_STOP)
        }
        if (useFastForwardAction) {
            stringActions.add(ACTION_FAST_FORWARD)
        }
        if (useNextAction) {
            stringActions.add(ACTION_NEXT)
        }
        return stringActions
    }

    private fun shouldShowPauseButton(player: Player): Boolean {
        return player.playbackState != Player.STATE_ENDED && player.playbackState != Player.STATE_IDLE && player.playWhenReady
    }

    class Builder(context: Context, notificationId: Int, channelId: String): PlayerNotificationManager.Builder(context, notificationId, channelId) {
        override fun build(): InternalNotificationManager {
            if (channelNameResourceId != 0) {
                NotificationUtil.createNotificationChannel(
                    context,
                    channelId,
                    channelNameResourceId,
                    channelDescriptionResourceId,
                    channelImportance
                )
            }
            return InternalNotificationManager(
                context,
                channelId,
                notificationId,
                mediaDescriptionAdapter,
                notificationListener,
                primaryActionReceiver,
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
