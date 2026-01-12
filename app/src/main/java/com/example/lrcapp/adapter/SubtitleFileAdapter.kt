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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SubtitleFileAdapter(
    private val files: MutableList<SubtitleFile>,
    private val onDownloadClick: (SubtitleFile) -> Unit
) : RecyclerView.Adapter<SubtitleFileAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvOutputFileName: TextView = itemView.findViewById(R.id.tvOutputFileName)
        val tvErrorMessage: TextView = itemView.findViewById(R.id.tvErrorMessage)
        val btnDownload: MaterialButton = itemView.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtitle_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        
        holder.tvFileName.text = file.fileName
        
        when (file.status) {
            FileStatus.PENDING -> {
                holder.tvStatus.text = "待處理"
                holder.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                )
                holder.tvOutputFileName.visibility = View.GONE
                holder.tvErrorMessage.visibility = View.GONE
                holder.btnDownload.visibility = View.GONE
            }
            FileStatus.PROCESSING -> {
                holder.tvStatus.text = "處理中"
                holder.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_light)
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                )
                holder.tvOutputFileName.visibility = View.GONE
                holder.tvErrorMessage.visibility = View.GONE
                holder.btnDownload.visibility = View.GONE
            }
            FileStatus.SUCCESS -> {
                holder.tvStatus.text = "成功"
                holder.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                )
                if (file.outputFileName != null) {
                    holder.tvOutputFileName.text = "輸出: ${file.outputFileName}"
                    holder.tvOutputFileName.visibility = View.VISIBLE
                }
                holder.tvErrorMessage.visibility = View.GONE
                holder.btnDownload.visibility = View.VISIBLE
                holder.btnDownload.setOnClickListener { onDownloadClick(file) }
            }
            FileStatus.ERROR -> {
                holder.tvStatus.text = "錯誤"
                holder.tvStatus.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                )
                holder.tvOutputFileName.visibility = View.GONE
                if (file.errorMessage != null) {
                    holder.tvErrorMessage.text = file.errorMessage
                    holder.tvErrorMessage.visibility = View.VISIBLE
                }
                holder.btnDownload.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateFile(position: Int, file: SubtitleFile) {
        files[position] = file
        notifyItemChanged(position)
    }

    fun addFile(file: SubtitleFile) {
        files.add(file)
        notifyItemInserted(files.size - 1)
    }

    fun clearFiles() {
        files.clear()
        notifyDataSetChanged()
    }
}
