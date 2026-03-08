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
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class SubtitleFileAdapter(
    private val files: MutableList<SubtitleFile>
) : RecyclerView.Adapter<SubtitleFileAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvFileMeta: TextView = itemView.findViewById(R.id.tvFileMeta)
        val tvFileFormat: TextView = itemView.findViewById(R.id.tvFileFormat)
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

        holder.tvFileName.text = file.fileName
        holder.tvFileMeta.text = formatFileSize(file.fileSize)
        holder.tvFileFormat.text = extractFormat(file.fileName)

        when (file.status) {
            FileStatus.PENDING -> bindStatus(
                holder = holder,
                label = "待處理",
                backgroundRes = R.drawable.status_background_pending,
                textColorRes = R.color.status_pending_text,
                strokeColorRes = R.color.outline_subtle,
                outputText = null,
                errorText = null
            )

            FileStatus.INVALID -> bindStatus(
                holder = holder,
                label = "無效",
                backgroundRes = R.drawable.status_background_error,
                textColorRes = R.color.status_error_text,
                strokeColorRes = R.color.status_error_bg,
                outputText = null,
                errorText = file.errorMessage
            )

            FileStatus.PROCESSING -> bindStatus(
                holder = holder,
                label = "處理中",
                backgroundRes = R.drawable.status_background_processing,
                textColorRes = R.color.status_processing_text,
                strokeColorRes = R.color.status_processing_bg,
                outputText = null,
                errorText = null
            )

            FileStatus.SUCCESS -> bindStatus(
                holder = holder,
                label = "成功",
                backgroundRes = R.drawable.status_background_success,
                textColorRes = R.color.status_success_text,
                strokeColorRes = R.color.status_success_bg,
                outputText = file.outputFileName?.let { "輸出: $it" },
                errorText = null
            )

            FileStatus.ERROR -> bindStatus(
                holder = holder,
                label = "失敗",
                backgroundRes = R.drawable.status_background_error,
                textColorRes = R.color.status_error_text,
                strokeColorRes = R.color.status_error_bg,
                outputText = null,
                errorText = file.errorMessage
            )
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateFile(position: Int, file: SubtitleFile) {
        files[position] = file
        notifyItemChanged(position)
    }

    private fun bindStatus(
        holder: ViewHolder,
        label: String,
        backgroundRes: Int,
        textColorRes: Int,
        strokeColorRes: Int,
        outputText: String?,
        errorText: String?
    ) {
        val context = holder.itemView.context
        holder.tvStatus.text = label
        holder.tvStatus.setBackgroundResource(backgroundRes)
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, textColorRes))
        holder.cardView.strokeColor = ContextCompat.getColor(context, strokeColorRes)

        if (outputText != null) {
            holder.tvOutputFileName.text = outputText
            holder.tvOutputFileName.visibility = View.VISIBLE
        } else {
            holder.tvOutputFileName.visibility = View.GONE
        }

        if (errorText != null) {
            holder.tvErrorMessage.text = errorText
            holder.tvErrorMessage.visibility = View.VISIBLE
        } else {
            holder.tvErrorMessage.visibility = View.GONE
        }
    }

    private fun extractFormat(fileName: String): String {
        return fileName.substringAfterLast('.', "")
            .ifEmpty { "未知" }
            .uppercase(Locale.ROOT)
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0L) {
            return "0 B"
        }
        if (sizeInBytes < 1024L) {
            return "$sizeInBytes B"
        }
        val sizeInKb = sizeInBytes / 1024.0
        if (sizeInKb < 1024.0) {
            return String.format(Locale.US, "%.0f KB", sizeInKb)
        }
        return String.format(Locale.US, "%.1f MB", sizeInKb / 1024.0)
    }
}
