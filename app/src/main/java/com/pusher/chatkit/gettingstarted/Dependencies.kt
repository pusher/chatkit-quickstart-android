package com.pusher.chatkit.gettingstarted

import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.CurrentUser

object Dependencies {
    var chatManager: ChatManager? = null
        set(value) {
            field?.close(callback = {})
            currentUser = null
            field = value
        }

    var currentUser: CurrentUser? = null
}
