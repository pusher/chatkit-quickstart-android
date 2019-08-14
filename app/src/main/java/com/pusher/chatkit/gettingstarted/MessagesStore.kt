package com.pusher.chatkit.gettingstarted

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.NewPart


class MessagesStore(
    private val currentUserId: String,
    private val currentUserName: String?,
    private val currentUserAvatarUrl: String?
) {
    enum class LocalMessageState {
        PENDING,
        FAILED,
        SENT
    }

    sealed class MessageItem {
        data class FromServer(val message: Message) : MessageItem()
        data class Local(val message: List<NewPart>, val state: LocalMessageState) : MessageItem()

        val internalId: String?
            get() = when (this) {
                is FromServer -> MessageMapper.messageToInternalId(message)
                is Local -> MessageMapper.messageToInternalId(message)
            }
    }

    data class MessagesModel(
        val currentUserId: String,
        val currentUserName: String?,
        val currentUserAvatarUrl: String?,
        val items: List<MessageItem>,
        val lastChange: ChangeType
    )

    private val items: MutableList<MessageItem> = mutableListOf()

    private val _model: MutableLiveData<MessagesModel> = MutableLiveData()
    val model: LiveData<MessagesModel> get() = _model

    fun addMessageFromServer(message: Message) {
        addOrUpdateItem(MessageItem.FromServer(message))
    }

    fun addPendingMessage(message: List<NewPart>) {
        addOrUpdateItem(MessageItem.Local(message, LocalMessageState.PENDING))
    }

    fun pendingMessageSent(message: List<NewPart>) {
        addOrUpdateItem(MessageItem.Local(message, LocalMessageState.SENT))
    }

    fun pendingMessageFailed(message: List<NewPart>) {
        addOrUpdateItem(MessageItem.Local(message, LocalMessageState.FAILED))
    }

    private fun addOrUpdateItem(item: MessageItem) {
        when (item) {
            is MessageItem.FromServer -> {
                // A message from the server is canonical, and will always replace a local message
                // with the same internalId
                val index = findItemIndexByInternalId(item.internalId)

                if (index == -1) {
                    addItem(item)
                } else {
                    replaceItem(item, index)
                }
            }
            is MessageItem.Local -> {
                // We may update the state of local messages, but we should never overwrite that of
                // a FromServer message
                val index = findItemIndexByInternalId(item.internalId)

                if (index == -1) {
                    addItem(item)
                } else if (items[index] !is MessageItem.FromServer) {
                    replaceItem(item, index)
                }
            }
        }
    }

    private fun addItem(item: MessageItem) {
        items.add(item)
        _model.postValue(
            MessagesModel(
                currentUserId,
                currentUserName,
                currentUserAvatarUrl,
                items,
                ChangeType.ItemAdded(items.size - 1)
            )
        )
    }

    private fun replaceItem(item: MessageItem, index: Int) {
        items[index] = item
        _model.postValue(
            MessagesModel(
                currentUserId,
                currentUserName,
                currentUserAvatarUrl,
                items,
                ChangeType.ItemUpdated(index)
            )
        )
    }

    private fun findItemIndexByInternalId(internalId: String?): Int =
        items.indexOfLast { item ->
            when (item) {
                is MessageItem.FromServer -> item.internalId != null && item.internalId == internalId
                is MessageItem.Local -> item.internalId != null && item.internalId == internalId
            }
        }
}