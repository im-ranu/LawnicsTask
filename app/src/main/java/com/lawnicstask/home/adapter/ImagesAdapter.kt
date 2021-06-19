package com.lawnicstask.home.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.lawnicstask.R
import com.lawnicstask.camera.model.ImageItems

class ImagesAdapter(var mContext : Context, var imageList : ArrayList<ImageItems>) : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {
    class ViewHolder(itemView : View)  : RecyclerView.ViewHolder(itemView) {

        var iv_uploaded = itemView.findViewById<ImageView>(R.id.uploadedImage)
        var tv_uploadedName = itemView.findViewById<TextView>(R.id.uploadedImageName)
        var tv_uploadedType = itemView.findViewById<TextView>(R.id.uploadedType)
        var tv_uploadedDate = itemView.findViewById<TextView>(R.id.uploadedDate)
        var tv_pages = itemView.findViewById<TextView>(R.id.docPages)
        var tv_time = itemView.findViewById<TextView>(R.id.docTime)

        fun bind(imageItems: ImageItems){
            iv_uploaded.load(imageItems.imgUrl){
                placeholder(R.drawable.ic_image_placeholder_black)
            }
            tv_uploadedName.setText(imageItems.imageName)
            tv_uploadedType.setText(imageItems.imgType)
            tv_uploadedDate.setText(imageItems.date)
            tv_pages.setText("Pages : "+imageItems.pages)
            tv_time.setText(imageItems.uploadedTime)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val inflate = LayoutInflater.from(parent.context).inflate(R.layout.item_image,parent,false)
        return ViewHolder(inflate)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Log.e("Position",position.toString())

        holder.bind(imageList[position])

    }

    override fun getItemCount(): Int {
        return imageList.size
    }
}