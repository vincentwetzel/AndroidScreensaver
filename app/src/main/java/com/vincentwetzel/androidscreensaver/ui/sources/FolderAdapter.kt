package com.vincentwetzel.androidscreensaver.ui.sources

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.vincentwetzel.androidscreensaver.R
import com.vincentwetzel.androidscreensaver.data.model.PhotoFolder

/**
 * Adapter for displaying folder list with checkboxes
 */
class FolderAdapter(
    private val onSelectionChanged: (Set<String>) -> Unit,
    private val onFolderClick: (String) -> Unit,
    private val onDeselectionChanged: ((Set<String>) -> Unit)? = null,
    private val onFolderChecked: (String, Boolean) -> Unit = { _, _ -> },
    private val mediaFilter: String? = null
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private val folders = mutableListOf<PhotoFolder>()
    private val selectedFolderIds = mutableSetOf<String>()
    private val deselectedFolderIds = mutableSetOf<String>()
    // Parent folder ID we're currently browsing inside of (null = root)
    private var currentParentFolderId: String? = null

    /**
     * Set the parent folder context. Subfolders of a selected parent are auto-checked.
     */
    fun setCurrentParentFolderId(parentId: String?) {
        currentParentFolderId = parentId
    }

    /**
     * Set the deselected folder IDs (explicitly unchecked by user).
     */
    fun setDeselectedFolders(ids: Set<String>) {
        deselectedFolderIds.clear()
        deselectedFolderIds.addAll(ids)
        notifyDataSetChanged()
    }

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

    fun getDeselectedFolders(): Set<String> {
        return deselectedFolderIds.toSet()
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

    /**
     * Get the correct label based on the content filter: "photos", "videos", or "items"
     */
    private fun getMediaLabel(): String = when (mediaFilter) {
        "images" -> "photos"
        "videos" -> "videos"
        else -> "items"
    }

    /**
     * Cascade selection: when a folder is checked/unchecked, add/remove all its
     * visible child folder IDs to/from the selected set.
     */
    fun cascadeSelection(folderId: String, isChecked: Boolean, childFolderIds: Set<String>) {
        if (isChecked) {
            // Add all children to selected, remove from deselected
            selectedFolderIds.addAll(childFolderIds)
            deselectedFolderIds.removeAll(childFolderIds)
        } else {
            // Remove all children from selected, add to deselected
            selectedFolderIds.removeAll(childFolderIds)
            deselectedFolderIds.addAll(childFolderIds)
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedFolderIds)
        onDeselectionChanged?.invoke(deselectedFolderIds)
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
        private val ivFolderIcon: ImageView = itemView.findViewById(R.id.iv_folder_icon)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_selected)

        fun bind(folder: PhotoFolder) {
            tvFolderName.text = folder.name
            tvPhotoCount.text = "${folder.photoCount} ${getMediaLabel()}"

            // Auto-check subfolders when their parent folder is selected
            val isInheritedSelection = currentParentFolderId != null &&
                    selectedFolderIds.contains(currentParentFolderId)
            val isExplicitlyDeselected = deselectedFolderIds.contains(folder.id)
            val isChecked = selectedFolderIds.contains(folder.id) ||
                    (isInheritedSelection && !isExplicitlyDeselected)

            checkbox.isChecked = isChecked

            // Load folder thumbnail if available
            if (!folder.thumbnailUri.isNullOrEmpty()) {
                ivFolderIcon.load(folder.thumbnailUri) {
                    crossfade(true)
                    placeholder(R.drawable.ic_folder)
                    error(R.drawable.ic_folder)
                    transformations(RoundedCornersTransformation(8f))
                }
            } else {
                ivFolderIcon.setImageResource(R.drawable.ic_folder)
            }

            // Use click listener instead of OnCheckedChangeListener to avoid triggering during bind()
            checkbox.setOnClickListener {
                val isNowChecked = checkbox.isChecked

                if (isNowChecked) {
                    selectedFolderIds.add(folder.id)
                    deselectedFolderIds.remove(folder.id)
                } else {
                    // Remove from selected and add to deselected
                    selectedFolderIds.remove(folder.id)
                    deselectedFolderIds.add(folder.id)
                }
                onSelectionChanged(selectedFolderIds)
                onDeselectionChanged?.invoke(deselectedFolderIds)
                onFolderChecked(folder.id, isNowChecked)
            }

            // Clicking anywhere on the row navigates into the folder
            itemView.setOnClickListener {
                onFolderClick(folder.id)
            }
        }
    }
}
