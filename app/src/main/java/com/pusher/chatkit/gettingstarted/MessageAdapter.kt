package com.pusher.chatkit.gettingstarted

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

internal class MessageAdapter(
    private val onClickListener: (position: Int) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var items: List<MessagesViewModel.MessageView> = listOf()

    fun update(messagesView: MessagesViewModel.MessagesView) {
        this.items = messagesView.items
        when (messagesView.change) {
            is ChangeType.ItemAdded -> notifyItemInserted(messagesView.change.index)
            is ChangeType.ItemUpdated -> notifyItemChanged(messagesView.change.index)
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].viewType.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout =
            when (viewType) {
                MessagesViewModel.ViewType.PENDING.ordinal -> R.layout.layout_message_row_pending
                MessagesViewModel.ViewType.FAILED.ordinal -> R.layout.layout_message_row_failed
                MessagesViewModel.ViewType.FROM_ME.ordinal -> R.layout.layout_message_row_me
                MessagesViewModel.ViewType.FROM_OTHER.ordinal -> R.layout.layout_message_row
                else -> throw Error("Unrecognised view type $viewType")
            }

        return MessageViewHolder(
            LayoutInflater.from(parent.context).inflate(layout, parent, false),
            onClickListener
        )
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }


    internal class MessageViewHolder(
        itemView: View,
        private val clickListener: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(messageView: MessagesViewModel.MessageView) {
            val lblSender = itemView.findViewById<TextView>(R.id.lblSender)
            val lblMessage = itemView.findViewById<TextView>(R.id.lblMessage)
            val imgAvatar = itemView.findViewById<ImageView>(R.id.imgAvatar)

            lblSender.text = messageView.senderName
            lblMessage.text = messageView.text

            if (messageView.senderAvatarUrl != null) {
                Picasso.get().load(messageView.senderAvatarUrl).into(imgAvatar)
            } else {
                imgAvatar.setImageDrawable(null)
            }
        }

        override fun onClick(v: View?) {
            clickListener(this.adapterPosition)
        }
    }
}
