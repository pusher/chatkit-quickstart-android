package com.pusher.chatkit.gettingstarted

sealed class ChangeType {
    data class ItemAdded(val index: Int) : ChangeType()
    data class ItemUpdated(val index: Int) : ChangeType()
}
