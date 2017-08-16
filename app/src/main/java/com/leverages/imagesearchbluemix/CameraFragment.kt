package com.leverages.imagesearchbluemix

/**
 * Created by takeo.kusama on 2017/07/10.
 */

import android.Manifest
import android.app.*
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.media.MediaActionSound
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.content.pm.PackageManager
import android.support.v13.app.FragmentCompat
import android.app.DialogFragment
import java.util.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.support.v4.content.ContextCompat
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions

class CameraFragment : Fragment(),View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {


    interface CameraFragmentInterface {
        // TODO: Update argument type and name
        fun onCameraFragmentInteraction(fragment:CameraFragment)
    }

    var pause = false
    var resultQuery = ""
    private var mTextureView: TextureView? = null
    private var mBackgroundThread : HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mImageReader:ImageReader? = null
    private var mCameraInfo:CameraInfo? = null
    private var mCameraDevice:CameraDevice? = null
    private var mCaptureSession:CameraCaptureSession? = null
    private lateinit var mCaptureRequestBuilder:CaptureRequest.Builder
    private lateinit var mCaptureRequest:CaptureRequest
    private lateinit var mSound:MediaActionSound
    private val FRAGMENT_DIALOG = "dialog"
    private var vr:VisualRecognition = VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20)

    override fun onClick(_v: View?) {
        takePicture()
    }

    override fun onDestroy(){
        super.onDestroy()
        stopCamera()
    }

    private val mOnImageListener = ImageReader.OnImageAvailableListener { reader ->
        if (!pause) {
            pause = true
            val image = reader.acquireLatestImage()
            val buffer = image.planes.first().buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            image.close()
            var options: ClassifyImagesOptions = ClassifyImagesOptions.Builder()
                    .classifierIds(VR_USE_CLASSIFIERS)
                    .images(data, "test.jpeg")
                    .build();
            var result = vr.classify(options).execute().toString()
            WatsonParser.vrResponseParse(result)?.let {
                resultQuery = it
                (activity as CameraFragmentInterface).onCameraFragmentInteraction(this)
                pause = false
            } ?: run {
                Toast.makeText(activity, "画像が認識できませんでした", Toast.LENGTH_LONG).show()
                pause = false
                mCaptureSession?.setRepeatingRequest(mCaptureRequest, mCaptureCallback,
                        mBackgroundHandler)
            }
        }
    }

    private val mStateCallBack:CameraDevice.StateCallback = object: CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice?) {
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }


        override fun onDisconnected(cameraDevice : CameraDevice?) {
            cameraDevice?.close()
            mCameraDevice = null
        }


        override fun onError(cameraDevice: CameraDevice?, error: Int) {
            cameraDevice?.close()
            mCameraDevice = null
            if(activity!=null) activity.finish()
        }
    }

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {


        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
        }

    }

    private var mSessionStateCallback:CameraCaptureSession.StateCallback = object : CameraCaptureSession.StateCallback(){

        override fun onConfigured(cameraCaptureSession: CameraCaptureSession?) {
            if(mCameraDevice == null){
                return;
            }
            mCaptureSession = cameraCaptureSession
            try {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                mCaptureRequest = mCaptureRequestBuilder.build()
                mCaptureSession!!.setRepeatingRequest(mCaptureRequest,mCaptureCallback,mBackgroundHandler)
            }catch (e:CameraAccessException){
                e.printStackTrace()
            }
        }
        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSound = MediaActionSound()
        mSound.load(MediaActionSound.SHUTTER_CLICK)
    }

    override fun onPause() {
        stopCamera()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        vr.setApiKey(WATSON_API_KEY)
        startCamera()
    }


    fun stopCamera() {
        mCaptureSession?.let{
            mCaptureSession!!.close()
        }

        mCameraDevice?.let {
            mCameraDevice!!.close()
            mCameraDevice = null
        }

        mImageReader?.close()

        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        stopBackgroundThread()
    }

    private fun startCamera(){
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread?.start()
        mBackgroundHandler = Handler(mBackgroundThread?.looper)

        if(mTextureView!!.isAvailable){
            openCamera(mTextureView!!.width,mTextureView!!.height)
        }else{
            mTextureView!!.surfaceTextureListener =  mTextureListener
        }
    }

    private val mTextureListener:TextureView.SurfaceTextureListener = object :TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, width: Int, height: Int) {
            openCamera(width,height)
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, width: Int, height: Int) {
            transformTexture(width,height)
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {

        }
    }

    private fun openCamera(width:Int, height:Int){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        mCameraInfo = CameraChooser(activity,width,height).chooseCamera()
        mCameraInfo?.let{
            val size = mCameraInfo!!.getPictureSize()
            mImageReader = ImageReader.newInstance(size.width,size.height,
                    ImageFormat.JPEG,2)
            mImageReader!!.setOnImageAvailableListener(mOnImageListener,mBackgroundHandler)
            transformTexture(width,height)
            var manager : CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                manager.openCamera(mCameraInfo!!.getCameraId(),mStateCallBack,mBackgroundHandler)
            }catch (e : CameraAccessException){
                e.printStackTrace()
            }
        }
    }
    private fun requestCameraPermission() {
        if (FragmentCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager , FRAGMENT_DIALOG)
        } else {
            FragmentCompat.requestPermissions(this, arrayOf<String>(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION);
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission)).show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        var view = inflater!!.inflate(R.layout.fragment_camera, container, false)
        view.setOnClickListener(this)
        return view
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        mTextureView = view!!.findViewById(R.id.PreviewTexture)
    }

    private fun createCameraPreviewSession(){
        try{
            var texture:SurfaceTexture = mTextureView!!.surfaceTexture
            val size = mCameraInfo!!.getPreviewSize()
            texture.setDefaultBufferSize(size.width,size.height)
            val surface = Surface(texture)
            mCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mCaptureRequestBuilder.addTarget(surface)
            mCameraDevice!!.createCaptureSession(Arrays.asList(surface,mImageReader!!.surface), mSessionStateCallback,null)
        }catch (e :CameraAccessException){
            e.printStackTrace()
        }
    }

    private fun transformTexture(viewWidth:Int, viewHeight: Int){
        if(mTextureView==null || mCameraInfo==null || activity ==null){
            return
        }
        var rotation:Int = activity.windowManager.defaultDisplay.rotation
        var viewSize = Size(viewWidth,viewHeight)
        var matrix:Matrix = Matrix()
        var viewRect = RectF(0F, 0F, viewSize.width.toFloat(), viewSize.height.toFloat())
        var size = mCameraInfo!!.getPictureSize()
        var bufferRect = RectF(0F,0F,size.height.toFloat(), size.width.toFloat())
        var centerX:Float = viewRect.centerX()
        var centerY:Float = viewRect.centerY()
        if(rotation == Surface.ROTATION_90|| rotation == Surface.ROTATION_270 ){
            bufferRect.offset(centerX-bufferRect.centerX(),centerY-bufferRect.centerY())
            matrix.setRectToRect(viewRect,bufferRect,Matrix.ScaleToFit.FILL)
            var scale = Math.max(viewSize.height/mCameraInfo!!.getPictureSize().height,
                    viewSize.width/mCameraInfo!!.getPictureSize().width)
            matrix.postScale(scale.toFloat(), scale.toFloat(),centerX,centerY)
            matrix.postRotate(90F*(rotation-2),centerX,centerY)
        }else if(rotation == Surface.ROTATION_180){
            matrix.postRotate(180F,centerX,centerY)
        }
        mTextureView!!.setTransform(matrix)
    }

    private fun takePicture(){
        try{
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)
            captureStillPicture()
        }catch(e:CameraAccessException){
            e.printStackTrace()
        }
    }

    private fun captureStillPicture(){
        try{
            if(activity==null||mCameraDevice==null){
               return
            }
            var captureBuilder:CaptureRequest.Builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader!!.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            var rotation:Int = activity.windowManager.defaultDisplay.rotation;
            var sensorOrientation = mCameraInfo!!.getSensorOrientation()
            var jpegRotation:Int = getPictureRotation(rotation,sensorOrientation)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,jpegRotation)
            var captureCallBack :CameraCaptureSession.CaptureCallback = object:CameraCaptureSession.CaptureCallback(){
                override fun onCaptureCompleted(session: CameraCaptureSession?, request: CaptureRequest?, result: TotalCaptureResult?) {
                    unlockFocus()
                }
            }
            mSound.play(MediaActionSound.SHUTTER_CLICK)
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.capture(captureBuilder.build(), captureCallBack ,mBackgroundHandler)
        }catch (e:CameraAccessException){
            e.printStackTrace()
        }
    }

    fun getPictureRotation(deviceRotation:Int,sensorOrientation:Int):Int{
        when(deviceRotation){
            Surface.ROTATION_0 -> return sensorOrientation
            Surface.ROTATION_90 -> return (sensorOrientation+270)%360
            Surface.ROTATION_180 -> return (sensorOrientation+180)%360
            Surface.ROTATION_270 -> return (sensorOrientation+90)%360
        }
        return 0
    }


    private fun unlockFocus(){
        try{
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            mCaptureSession?.capture(mCaptureRequestBuilder.build(),null,mBackgroundHandler )
        }catch (e:CameraAccessException){
            e.printStackTrace()
        }
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler?.removeCallbacks(null)
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    class ConfirmationDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(activity)
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        FragmentCompat.requestPermissions(parentFragment,arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION) }
                    .setNegativeButton(android.R.string.cancel
                    ) { dialog, which ->
                        parentFragment.activity?.finish()
                    }
                    .create()
        }
    }

    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                    .setMessage(arguments.getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i -> activity.finish() }
                    .setNegativeButton(android.R.string.cancel)
                    { dialog, which ->
                        parentFragment.activity?.finish()
                    }
                    .create()
        }

        companion object {

            private val ARG_MESSAGE = "message"

            fun newInstance(message: String): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }

    }

    companion object {
        val REQUEST_CAMERA_PERMISSION : Int = 1
    }
}// Required empty public constructor
