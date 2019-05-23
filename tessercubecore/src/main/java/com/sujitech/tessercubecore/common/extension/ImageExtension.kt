package com.sujitech.tessercubecore.common.extension

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import java.io.File

fun ImageView.load(path: String) {
    Glide.with(this).load(path).centerCrop().into(this)
}
fun ImageView.load(bitmap: Bitmap) {
    Glide.with(this).load(bitmap).centerCrop().into(this)
}
fun ImageView.load(drawable: Drawable) {
    Glide.with(this).load(drawable).centerCrop().into(this)
}
fun ImageView.load(@DrawableRes id: Int) {
    Glide.with(this).load(id).centerCrop().into(this)
}
fun ImageView.load(file: File) {
    Glide.with(this).load(file).centerCrop().into(this)
}