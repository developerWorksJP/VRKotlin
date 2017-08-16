package com.leverages.imagesearchbluemix

import android.util.Size

/**
 * Created by takeo.kusama on 2017/07/10.
 */

class CameraInfo {
    private lateinit var cameraId:String
    private lateinit var previewSize:Size
    private lateinit var pictureSize:Size
    private var sensorOrientation:Int = 0
    fun getCameraId() :String{
        return cameraId
    }
    fun setCameraId(cameraId:String) {
        this.cameraId = cameraId
    }
    fun getPreviewSize() :Size{
        return previewSize
    }
    fun setPreviewSize(previewSize:Size) {
        this.previewSize = previewSize
    }
    fun getPictureSize() :Size{
        return pictureSize
    }
    fun setPictureSize(pictureSize:Size) {
        this.pictureSize = pictureSize
    }
    fun getSensorOrientation() :Int{
        return sensorOrientation
    }
    fun setSensorOrientation(sensorOrientation:Int) {
        this.sensorOrientation = sensorOrientation
    }
}