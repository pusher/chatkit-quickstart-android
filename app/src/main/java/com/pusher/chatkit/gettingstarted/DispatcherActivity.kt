package com.pusher.chatkit.gettingstarted

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pusher.chatkit.AndroidChatkitDependencies
import com.pusher.chatkit.ChatListeners
import com.pusher.chatkit.ChatManager
import com.pusher.chatkit.ChatkitTokenProvider
import com.pusher.util.Result

class DispatcherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dispatcher)

        // create the chat manager
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
        Dependencies.chatManager = chatManager

        // attempt to connect
        chatManager.connect(
            listeners = ChatListeners(),
            callback = { result ->
                runOnUiThread {
                    when (result) {
                        is Result.Success -> {
                            Dependencies.currentUser = result.value
                            startActivity(Intent(this, MessagesActivity::class.java))
                            finish()
                        }
                        is Result.Failure -> {
                            Toast.makeText(this, result.error.reason, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
