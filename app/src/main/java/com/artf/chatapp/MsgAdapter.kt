package com.artf.chatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.databinding.ItemMessageBinding
import com.artf.chatapp.model.Message

class MsgAdapter(private val clickListener: OnClickListener) : ListAdapter<Message,
        MsgAdapter.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(clickListener, product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    class ViewHolder constructor(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: OnClickListener, item: Message) {
            binding.message = item
            //binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    companion object GridViewDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    class OnClickListener(val clickListener: (productId: Message) -> Unit) {
        fun onClick(product: Message) = clickListener(product)
    }
}