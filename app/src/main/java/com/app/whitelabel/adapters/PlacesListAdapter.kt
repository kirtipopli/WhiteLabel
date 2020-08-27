package com.app.whitelabel.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.app.whitelabel.R
import com.app.whitelabel.databinding.CustomInfoContentsBinding
import com.app.whitelabel.model.PlaceDetails

class PlacesListAdapter(
    private val mContext: Context, var placesList: ArrayList<PlaceDetails>?,
    var onPlaceSelected: (placeDetails: PlaceDetails?, position: Int) -> Unit
) :
    RecyclerView.Adapter<PlacesListAdapter.PlaceViewHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlacesListAdapter.PlaceViewHolder {
        val mBinding = DataBindingUtil.inflate<CustomInfoContentsBinding>(
            LayoutInflater.from(mContext)
            , R.layout.custom_info_contents, parent, false
        )
        return PlaceViewHolder(mBinding.root)
    }

    override fun getItemCount(): Int {
        return placesList?.size ?: 0
    }

    override fun onBindViewHolder(holder: PlacesListAdapter.PlaceViewHolder, position: Int) {

        if (!placesList.isNullOrEmpty()) {

            var placeDetails: PlaceDetails? = placesList?.get(position)

            if (placeDetails != null) {
                holder.binding?.title?.text = placeDetails.title
                holder.binding?.snippet?.text = placeDetails.snippeet

                holder.binding?.rlListItemMain?.setOnClickListener {
                    onPlaceSelected(placeDetails, position)
                }
            }
        }

    }

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val binding: CustomInfoContentsBinding? = DataBindingUtil.bind(itemView)
    }

}