package com.pusher.chatkit.gettingstarted

object Config {
    // TODO: supply your instance locator from the dashboard
    const val INSTANCE_LOCATOR = "YOUR_INSTANCE_LOCATOR"

    const val USER_ID = "pusher-quick-start-alice"

    val tokenProviderUrl: String
        get() {
            if (INSTANCE_LOCATOR == "YOUR_INSTANCE_LOCATOR") {
                throw RuntimeException("Config.kt needs to be updated with your instance locator")
            }
            return INSTANCE_LOCATOR.split(":").let { (_, cluster, instanceId) ->
                "https://$cluster.pusherplatform.io/services/chatkit_token_provider/v1/$instanceId/token"
            }
        }
}
