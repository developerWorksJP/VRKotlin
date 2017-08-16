package com.leverages.imagesearchbluemix

import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Size
import java.sql.Array
import java.util.*

/**
 * Created by takeo.kusama on 2017/07/10.
 */

class CameraChooser(var context: Context, var width:Int, var height:Int){

     fun chooseCamera():CameraInfo?{
        var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            var cameraIds = cameraManager.cameraIdList

            cameraIds.forEach {

                val characteristics : CameraCharacteristics = cameraManager.getCameraCharacteristics(it)
                if(!isBackFacing(characteristics)) return@forEach
                var map : StreamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return@forEach
                var pictureSize: Size = chooseImageSize(map) ?: return@forEach
                var previewSize: Size = choosePreviewSize(map) ?: return@forEach
                var sensorOrientation:Int = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: return@forEach
                var cameraInfo :CameraInfo = CameraInfo()
                cameraInfo.setCameraId(it)
                cameraInfo.setPictureSize(pictureSize)
                cameraInfo.setPreviewSize(previewSize)
                cameraInfo.setSensorOrientation(sensorOrientation)
                if(cameraInfo != null) return@chooseCamera cameraInfo

            }
        }catch(e:CameraAccessException){
            e.printStackTrace()
        }
        return null
    }

    private fun choosePreviewSize (map: StreamConfigurationMap):Size?{
        var previewSizes = map.getOutputSizes(SurfaceTexture::class.java)
        return getMinimalSize(width/2,height/2,previewSizes)
    }

    private fun chooseImageSize(map: StreamConfigurationMap):Size?{
        var pictureSizes = map.getOutputSizes(ImageFormat.JPEG)
        return getMinimalSize(width,height,pictureSizes)
    }

    private fun isBackFacing(characteristics : CameraCharacteristics):Boolean{
        var facing:Int = characteristics.get(CameraCharacteristics.LENS_FACING)
        return (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK)
    }

    private fun getMinimalSize(minWidth:Int, minHeight:Int, sizes:kotlin.Array<Size>):Size?{
        var sizeList:List<Size> = listOf<Size>(*sizes)
        Collections.sort(sizeList) { a, b -> a.height * a.width - b.height * b.width }
        var sizeMax: Size? = sizeList.firstOrNull {
            (it.width>=minWidth && it.height >= minHeight) || (it.width>=minHeight && it.height>=minWidth)
        } ?: return Collections.max(sizeList,{ lhs :Size, rhs:Size ->
            java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        })
        return sizeMax
    }

}