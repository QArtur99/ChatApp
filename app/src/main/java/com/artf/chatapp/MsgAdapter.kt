package com.artf.chatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.databinding.ItemMessageLeftImgBinding
import com.artf.chatapp.databinding.ItemMessageLeftRecordBinding
import com.artf.chatapp.databinding.ItemMessageLeftTextBinding
import com.artf.chatapp.databinding.ItemMessageRightImgBinding
import com.artf.chatapp.databinding.ItemMessageRightRecordBinding
import com.artf.chatapp.databinding.ItemMessageRightTextBinding
import com.artf.chatapp.model.Message

class MsgAdapter(
    private val viewLifecycleOwner: LifecycleOwner,
    private val msgAdapterInt: MsgAdapterInt
) : ListAdapter<Message, RecyclerView.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        (holder as MsgViewHolder<*>).bind(viewLifecycleOwner, msgAdapterInt, msg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_message_left_text -> MsgViewHolder(
                ItemMessageLeftTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_right_text -> MsgViewHolder(
                ItemMessageRightTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_left_img -> MsgViewHolder(
                ItemMessageLeftImgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_right_img -> MsgViewHolder(
                ItemMessageRightImgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_left_record -> MsgViewHolder(
                ItemMessageLeftRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_right_record -> MsgViewHolder(
                ItemMessageRightRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)!!
        return if (item.isOwner!!) {
            when {
                item.audioUrl != null -> R.layout.item_message_right_record
                item.photoUrl != null -> R.layout.item_message_right_img
                else -> R.layout.item_message_right_text
            }
        } else {
            when {
                item.audioUrl != null -> R.layout.item_message_left_record
                item.photoUrl != null -> R.layout.item_message_left_img
                else -> R.layout.item_message_left_text
            }
        }
    }

    class MsgViewHolder<T : ViewDataBinding> constructor(val binding: T) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewLifecycleOwner: LifecycleOwner, msgAdapterInt: MsgAdapterInt, item: Message) {
            binding.lifecycleOwner = viewLifecycleOwner
            binding.setVariable(BR.message, item)
            binding.setVariable(BR.msgAdapterInt, msgAdapterInt)
            binding.executePendingBindings()
            val seekBar = binding.root.findViewById<SeekBar>(R.id.seekBar)
            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    msgAdapterInt.onStartTrackingTouch(seekBar, item)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    msgAdapterInt.onStopTrackingTouch(seekBar, item)
                }
            })
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

    interface MsgAdapterInt {
        fun onAudioClick(view: View, message: Message)
        fun onStartTrackingTouch(seekBar: SeekBar, item: Message)
        fun onStopTrackingTouch(seekBar: SeekBar, item: Message)
        fun getVm(): FirebaseViewModel
        fun showPic(view: View, message: Message)
    }
}