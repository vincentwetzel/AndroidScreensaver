package com.vincentwetzel.androidscreensaver.ui.sources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder

/**
 * Adapter for displaying folder list with checkboxes
 */
class FolderAdapter(
    private val onSelectionChanged: (Set<String>) -> Unit,
    private val onFolderClick: (String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private val folders = mutableListOf<PhotoFolder>()
    private val selectedFolderIds = mutableSetOf<String>()

    fun submitList(newFolders: List<PhotoFolder>) {
        folders.clear()
        folders.addAll(newFolders)
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedFolderIds.clear()
        selectedFolderIds.addAll(folders.map { it.id })
        notifyDataSetChanged()
        onSelectionChanged(selectedFolderIds)
    }

    fun deselectAll() {
        selectedFolderIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedFolderIds)
    }

    fun getSelectedFolders(): Set<String> {
        return selectedFolderIds.toSet()
    }

    fun setSelectedFolders(ids: Set<String>) {
        selectedFolderIds.clear()
        selectedFolderIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun getPhotoCount(): Int {
        return folders.filter { selectedFolderIds.contains(it.id) }
            .sumOf { it.photoCount }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_checkbox, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFolderName: TextView = itemView.findViewById(R.id.tv_folder_name)
        private val tvPhotoCount: TextView = itemView.findViewById(R.id.tv_photo_count)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_selected)

        fun bind(folder: PhotoFolder) {
            tvFolderName.text = folder.name
            tvPhotoCount.text = "${folder.photoCount} photos"
            checkbox.isChecked = selectedFolderIds.contains(folder.id)

            // Use click listener instead of OnCheckedChangeListener to avoid triggering during bind()
            checkbox.setOnClickListener {
                val isChecked = checkbox.isChecked
                if (isChecked) {
                    selectedFolderIds.add(folder.id)
                } else {
                    selectedFolderIds.remove(folder.id)
                }
                onSelectionChanged(selectedFolderIds)
            }

            // Clicking anywhere on the row navigates into the folder
            itemView.setOnClickListener {
                onFolderClick(folder.id)
            }
        }
    }
}
