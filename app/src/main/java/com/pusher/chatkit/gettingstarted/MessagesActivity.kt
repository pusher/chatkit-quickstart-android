package com.pusher.chatkit.gettingstarted

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.pusher.chatkit.*
import com.pusher.chatkit.messages.multipart.Message
import com.pusher.chatkit.messages.multipart.NewPart
import com.pusher.chatkit.rooms.Room
import com.pusher.chatkit.rooms.RoomEvent
import com.pusher.chatkit.rooms.RoomListeners
import com.pusher.util.Result
import kotlinx.android.synthetic.main.activity_messages.*

// An activity which displays messages in a conversation view.
//
// To keep things simple, this class has multiple responsibilities which would most likely be
// split out in a larger application, for example:
//  - We recommend initialising the Chatkit SDK and connection once, and then injecting the
//    SDK in to activities which need access to it, but in this example the activity initialises
//    and connects as there are no others.
//  - This Activity is responsible for constructing a co-ordinating the data store and view models
class MessagesActivity : AppCompatActivity() {

    // Chatkit SDK main interaction handle
    private lateinit var currentUser: CurrentUser
    // The Room we are rendering messages for
    private lateinit var currentRoom: Room

    // The MessagesStore is responsible for tracking messages in the room
    private lateinit var messagesStore: MessagesStore

    // The MessagesViewModel translates the contents of the MessagesStore to the information we
    // want to render. This representation is then forwarded to the Adaptor for the RecyclerView
    private val messagesViewModel: MessagesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Instantiate Chatkit with instance ID, token provider endpoint, and ID of the user we
        // will connect as.
        // Initialization: https://pusher.com/docs/chatkit/reference/android#instantiate-chatkit
        // Authenticating users and and providing tokens:
        // https://pusher.com/docs/chatkit/reference/android#token-provider
        // This example uses the (insecure) test token provider, provided by Chatkit, which will
        // sign a token for any user who requests it. It is only suitable for private development
        // purposes.
        val chatManager = ChatManager(
            instanceLocator = Config.INSTANCE_LOCATOR,
            userId = Config.USER_ID,
            dependencies = AndroidChatkitDependencies(
                tokenProvider = ChatkitTokenProvider(
                    endpoint = Config.tokenProviderUrl,
                    userId = Config.USER_ID
                )
            )
        )

        // Connect to Chatkit, passing in a listener which will display errors as toasts, and a
        // callback to execute when the connection is complete.
        // https://pusher.com/docs/chatkit/reference/android#connecting-to-chatkit
        chatManager.connect(
            listeners = ChatListeners(
                onErrorOccurred = { error ->
                    runOnUiThread {
                        Toast.makeText(this, error.reason, Toast.LENGTH_SHORT).show()
                    }
                }
            ),
            callback = { result ->
                // Callbacks may come from a background thread, so we switch to the UI thread to
                // continue updating the Activity
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            this.onConnect(result.value)
                        }
                        is Result.Failure -> {
                            Toast.makeText(this, result.error.reason, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun onConnect(currentUser: CurrentUser) {
        this.currentUser = currentUser
        this.currentRoom = currentUser.rooms.first()

        // Configure the RecyclerView which will display messages
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewMessages.layoutManager = layoutManager

        // Create an adaptor for the RecyclerView which will pass taps on messages back to us
        val adapter = MessageAdapter(
            onClickListener = this::onClickMessage
        )
        recyclerViewMessages.adapter = adapter

        // Construct the store in which we will track messages, both those received from the backend
        // and messages which have not yet reached the backend.
        messagesStore = MessagesStore(
            currentUserId = currentUser.id,
            currentUserName = currentUser.name,
            currentUserAvatarUrl = currentUser.avatarURL
        )
        // Every time the contents of the store change, forward them to the ViewModel
        messagesStore.model.observe(
            this,
            Observer<MessagesStore.MessagesModel> { newDataModel ->
                messagesViewModel.update(newDataModel)
            })

        // Every time the contents of the ViewModel change, forward them to the adaptor
        messagesViewModel.model.observe(
            this,
            Observer<MessagesViewModel.MessagesView> { newViewModel ->
                adapter.update(newViewModel)

                // If the change was due to a *new* message, scroll it in to view
                when (newViewModel.change) {
                    is ChangeType.ItemAdded -> {
                        recyclerViewMessages.scrollToPosition(newViewModel.change.index)
                    }
                }
            })

        // Configure the text entry field for messages to support multiple ways of submitting
        // (including pressing return on a real keyboard, as we expect to be run in the Emulator)
        txtMessage.setOnEditorActionListener { _, actionId, event ->
            // Return pressed on external / simulator keyboard
            val returnPressed = event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER
            // Send pressed on soft keyboard
            val sendPressed = actionId == EditorInfo.IME_ACTION_SEND

            if (sendPressed || returnPressed) {
                sendMessageFromTextEntry()
                true
            } else {
                // We must declare we "handled" all events related to the enter key, otherwise
                // further events (like key-up) will move the cursor focus elsewhere.
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            }
        }

        // With the Store, ViewModel and RecyclerView ready, we can "subscribe" to the room
        // using the Chatkit SDK.
        // https://pusher.com/docs/chatkit/reference/android#subscriptions
        currentUser.subscribeToRoomMultipart(
            currentRoom.id,
            // Listeners are passed to be notified of events occurring in the room.
            // Every time a message is received, we add it to our store of messages.
            listeners = RoomListeners(
                onMultipartMessage = { message ->
                    runOnUiThread {
                        messagesStore.addMessageFromServer(message)
                    }
                }
            ),
            // Request up to 50 old messages to be sent to us when the subscription starts
            messageLimit = 50,
            // We don't need to do anything specific when the subscription is ready
            callback = {}
        )
    }

    // Event handler for the send button onClick
    fun onClickSendButton(view: View) {
        sendMessageFromTextEntry()
    }

    // Event handler for taps on messages in the view
    private fun onClickMessage(position: Int) {
        // Look up the message that was tapped
        val item = messagesStore.model.value?.items?.get(position)

        when (item) {
            is MessagesStore.MessageItem.Local -> {
                if (item.state == MessagesStore.LocalMessageState.FAILED) {
                    // If the message is a failed message, retry it
                    sendMessage(item.message)
                }
            }
        }
    }

    // Grab the contents of the text entry field and send it as a new message
    private fun sendMessageFromTextEntry() {
        val text = txtMessage.text.toString().trim()
        if (text.isEmpty()) {
            return
        }

        txtMessage.setText("")
        sendMessage(MessageMapper.textToNewMessage(text))
    }

    // Send a message
    private fun sendMessage(message: List<NewPart>) {
        // First, add it to the store as a "pending" message
        messagesStore.addPendingMessage(message)

        // Send the message to Chatkit
        // https://pusher.com/docs/chatkit/reference/android#sending-a-message
        currentUser.sendMultipartMessage(
            roomId = currentRoom.id,
            parts = message,
            callback = { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            // If it was accepted, mark it as sent in the Store
                            messagesStore.pendingMessageSent(message)
                        }
                        is Result.Failure -> {
                            // If it was rejected, mark it as failed in the Store
                            messagesStore.pendingMessageFailed(message)
                            Toast.makeText(
                                this,
                                "Message failed to send, tap it to retry",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )
    }
}
