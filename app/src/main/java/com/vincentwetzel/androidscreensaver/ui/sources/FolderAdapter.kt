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
    private val onSelectionStateChanged: (selectedIds: Set<String>, deselectedIds: Set<String>) -> Unit,
    private val onFolderClick: (String) -> Unit,
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
        val visibleIds = folders.map { it.id }
        selectedFolderIds.addAll(visibleIds)
        deselectedFolderIds.removeAll(visibleIds.toSet())
        notifyDataSetChanged()
        onSelectionStateChanged(selectedFolderIds.toSet(), deselectedFolderIds.toSet())
    }

    fun deselectAll() {
        val visibleIds = folders.map { it.id }
        selectedFolderIds.removeAll(visibleIds.toSet())
        deselectedFolderIds.addAll(visibleIds)
        notifyDataSetChanged()
        onSelectionStateChanged(selectedFolderIds.toSet(), deselectedFolderIds.toSet())
    }

    fun getSelectedFolders(): Set<String> {
        return selectedFolderIds.toSet()
    }

    fun getDeselectedFolders(): Set<String> {
        return deselectedFolderIds.toSet()
    }

    fun getVisibleFolders(): List<PhotoFolder> {
        return folders.toList()
    }

    fun setSelectedFolders(ids: Set<String>) {
        selectedFolderIds.clear()
        selectedFolderIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun getPhotoCount(): Int {
        val isInheritedSelection = currentParentFolderId != null && selectedFolderIds.contains(currentParentFolderId)
        return folders.filter { folder ->
            selectedFolderIds.contains(folder.id) || (isInheritedSelection && !deselectedFolderIds.contains(folder.id))
        }.sumOf { it.photoCount }
    }

    /**
     * Get the correct label based on the content filter: "photos", "videos", or "items"
     */
    private fun getMediaLabel(): String = when (mediaFilter) {
        "images" -> "photos"
        "videos" -> "videos"
        else -> "items"
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
            
            if (folder.photoCount > 0) {
                tvPhotoCount.visibility = View.VISIBLE
                tvPhotoCount.text = "${folder.photoCount} ${getMediaLabel()}"
            } else {
                tvPhotoCount.visibility = View.GONE
            }

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
                ivFolderIcon.load(R.drawable.ic_folder)
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
                onSelectionStateChanged(selectedFolderIds.toSet(), deselectedFolderIds.toSet())
            }

            // Clicking anywhere on the row navigates into the folder
            itemView.setOnClickListener {
                onFolderClick(folder.id)
            }
        }
    }
}
