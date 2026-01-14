package com.example.lrcapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lrcapp.R
import com.example.lrcapp.model.FileStatus
import com.example.lrcapp.model.SubtitleFile
import com.google.android.material.R as M3R

class SubtitleFileAdapter(
    private val files: MutableList<SubtitleFile>
) : RecyclerView.Adapter<SubtitleFileAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvOutputFileName: TextView = itemView.findViewById(R.id.tvOutputFileName)
        val tvErrorMessage: TextView = itemView.findViewById(R.id.tvErrorMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtitle_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val context = holder.itemView.context

        holder.tvFileName.text = file.fileName

        when (file.status) {
            FileStatus.PENDING -> {
                holder.tvStatus.text = "待處理"
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_pending)
                holder.tvStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, M3R.attr.colorOnSecondaryContainer, ContextCompat.getColor(context, android.R.color.black)))
                holder.tvOutputFileName.visibility = View.GONE
                holder.tvErrorMessage.visibility = View.GONE
            }
            FileStatus.PROCESSING -> {
                holder.tvStatus.text = "處理中"
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_processing)
                holder.tvStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, M3R.attr.colorOnPrimaryContainer, ContextCompat.getColor(context, android.R.color.black)))
                holder.tvOutputFileName.visibility = View.GONE
                holder.tvErrorMessage.visibility = View.GONE
            }
            FileStatus.SUCCESS -> {
                holder.tvStatus.text = "成功"
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_success)
                holder.tvStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, M3R.attr.colorOnTertiaryContainer, ContextCompat.getColor(context, android.R.color.black)))
                if (file.outputFileName != null) {
                    holder.tvOutputFileName.text = "輸出: ${file.outputFileName}"
                    holder.tvOutputFileName.visibility = View.VISIBLE
                }
                holder.tvErrorMessage.visibility = View.GONE
            }
            FileStatus.ERROR -> {
                holder.tvStatus.text = "錯誤"
                holder.tvStatus.setBackgroundResource(R.drawable.status_background_error)
                holder.tvStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(context, M3R.attr.colorOnErrorContainer, ContextCompat.getColor(context, android.R.color.black)))
                holder.tvOutputFileName.visibility = View.GONE
                if (file.errorMessage != null) {
                    holder.tvErrorMessage.text = file.errorMessage
                    holder.tvErrorMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateFile(position: Int, file: SubtitleFile) {
        files[position] = file
        notifyItemChanged(position)
    }
}
