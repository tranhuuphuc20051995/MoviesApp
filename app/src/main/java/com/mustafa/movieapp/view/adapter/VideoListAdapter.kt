package com.mustafa.movieapp.view.adapter

import android.view.View
import com.skydoves.baserecyclerviewadapter.BaseAdapter
import com.skydoves.baserecyclerviewadapter.BaseViewHolder
import com.skydoves.baserecyclerviewadapter.SectionRow
import com.mustafa.movieapp.R
import com.mustafa.movieapp.models.Resource
import com.mustafa.movieapp.models.Video
import com.mustafa.movieapp.view.viewholder.VideoListViewHolder

class VideoListAdapter(private val delegate: VideoListViewHolder.Delegate)
    : BaseAdapter() {

    init {
        addSection(ArrayList<Video>())
    }

    fun addVideoList(resource: Resource<List<Video>>) {
        resource.data?.let {
            sections()[0].addAll(it)
        }
        notifyDataSetChanged()
    }

    override fun layout(sectionRow: SectionRow): Int {
        return R.layout.item_video
    }

    override fun viewHolder(layout: Int, view: View): BaseViewHolder {
        return VideoListViewHolder(view, delegate)
    }
}
