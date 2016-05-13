package com.stomhong.customcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * <p>拍照页面</p>
 * 这里要注意一个问题，就是{@link Bundle}对象只能传递最大为40K的数据?
 * <ol>遇到的错误：JavaBinder: !!! FAILED BINDER TRANSACTION !!!</ol>
 *
 * @author StomHong
 * @since 2016-4-20
 */
public class TakePictureActivity extends Activity {

    private static final String TAG = TakePictureActivity.class.getSimpleName();
    private Camera mCamera;
    private SurfaceView mSurfaceView_Preview;
    private ImageView mImageView_TakePicture;
    private SurfaceHolder holder;
    private String mPathName;
    private int orientation;
    private MyOrientationDetector myOrientationDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        initView();
        initData();
        initEvent();
    }

    private void initData() {
        myOrientationDetector = new MyOrientationDetector(getApplicationContext());
        holder = mSurfaceView_Preview.getHolder();
    }

    private void initEvent() {


        mImageView_TakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(new MyShutterCallback(), null, new MyPictureCallback());

            }
        });
    }

    private void initView() {

        mSurfaceView_Preview = (SurfaceView) findViewById(R.id.id_sv_preview);
        mImageView_TakePicture = (ImageView) findViewById(R.id.id_btn_take_picture);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myOrientationDetector.enable();
        holder.setKeepScreenOn(true);
        //为SurfaceView的句柄添加一个回调函数
        holder.addCallback(new SurfaceCallback());
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        myOrientationDetector.disable();
    }

    /**
     * 快门回调接口
     */
    private class MyShutterCallback implements Camera.ShutterCallback {
        @Override
        public void onShutter() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mCamera.enableShutterSound(true);
            }
        }
    }


    /**
     * 预览回调接口
     */
    private class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            if (mCamera == null) {
                mCamera = Camera.open(0);
                try {
                    mCamera.setPreviewDisplay(holder);
                    setCameraDisplayOrientation(TakePictureActivity.this,0,mCamera);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            Parameters param = mCamera.getParameters();
            // 得到相机支持的所有预览尺寸
            List<Size> supporPreviewSizes = param.getSupportedPreviewSizes();
            param.setPictureFormat(PixelFormat.JPEG);
            param.setJpegQuality(100);
            Camera.Size preSize = getOptimalPreviewSize(param,supporPreviewSizes, width, height);
            param.setPreviewSize(preSize.width, preSize.height);

            DisplayMetrics outMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
            int screenWidth = outMetrics.widthPixels;
            int screenHeight = outMetrics.heightPixels;
            float screenScale = (float) screenWidth / screenHeight;
            Camera.Size picSize = getOptimalPictureSize(param.getSupportedPictureSizes(), screenScale,preSize);
            param.setPictureSize(picSize.width, picSize.height);
            mCamera.setParameters(param);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                    if (success) {
                        camera.cancelAutoFocus();
                    }
                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    /**
     * 获取最佳的照片尺寸
     * @param sizes
     * @param screenScale
     * @param previewSize
     * @return
     */

    private Size getOptimalPictureSize(List<Size> sizes, float screenScale,Size previewSize) {
        Size picSize = sizes.get(0);
        int h = 0;
        for (Size size : sizes) {
            Log.i("size", String.format("Pre:support picture size %d:<%d, %d>",
                    h++, size.width, size.height));
        }

        for (Size temSize : sizes) {
            float temp = (float) temSize.width / temSize.height;
            if (screenScale == temp
                    && (temSize.width * temSize.height) <= previewSize.height*previewSize.width) {
                //&& (temSize.width * temSize.height) <= MAXIMUM_SIZE) {
                return temSize;
            }
        }

        Collections.sort(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                return lhs.width * lhs.height - rhs.width * rhs.height;
            }
        });

        int j = 0;
        for (Size size : sizes) {
            Log.i("size", String.format("After sort:support picture size %d:<%d, %d>", j++,size.width, size.height));
        }

        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            if (size.width * size.height >= previewSize.height*previewSize.width && i > 0) {
                //(size.width * size.height >= MAXIMUM_SIZE && i > 0) {
                picSize = sizes.get(i - 1);
                break;
            }
        }

        if (sizes.contains(previewSize)
                && (previewSize.width * previewSize.height) <= previewSize.height*previewSize.width) {
            //&& (previewSize.width * previewSize.height) <= MAXIMUM_SIZE) {
            picSize = previewSize;
        }

//        Log.i("size",String.format("size:<%d, %d>", picSize.width, picSize.height));
        return picSize;
    }

    /**
     * 获得最佳的预览尺寸
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private Size getOptimalPreviewSize(Camera.Parameters params,List<Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 拍照回调接口
     */
    private class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            savePicture(data);
            Intent intent = new Intent(TakePictureActivity.this, PreviewActivity.class);
            intent.putExtra("pathName", mPathName);
            intent.putExtra("orientation",orientation);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 将照片文件保存到SDCard中
     *
     * @param data
     */
    private void savePicture(byte[] data) {
        String p = Environment.getExternalStorageDirectory().getPath();
        File file = createDirectory(p, "Youxiao");
        String path = file.getAbsolutePath();
        String fileNo = getSystemTime();
        String fileName = "P" + fileNo + ".jpg";
        mPathName = writeFileIntoSDCard(data, path, fileName);
    }

    /**
     * 将文件以I/O流的方式写入SDCard
     *
     * @param data
     * @param path
     * @param fileName
     */
    private String writeFileIntoSDCard(byte[] data, String path, String fileName) {
        File file = new File(path, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return path + File.separator + fileName;
    }

    /**
     * 在指定的目录下创建文件夹
     */
    private File createDirectory(String path, String dirName) {
        File file = new File(path, dirName);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    /**
     * 获取系统时间，返回特定的时间格式
     *
     * @return time 指定的时间格式
     */
    private String getSystemTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA);
        Date curDate = new Date(System.currentTimeMillis());
        return sdf.format(curDate);
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 设置相机的预览角度
     * @param activity
     * @param cameraId
     * @param camera
     */
    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {

            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;

            default:
                break;
        }
        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 屏幕旋转回调接口
     */
    public class MyOrientationDetector extends OrientationEventListener {

        public MyOrientationDetector(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            try {
                if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;  //手机平放时，检测不到有效的角度
                }
                //只检测是否有四个角度的改变
                if( orientation > 350 || orientation< 10 ) { //0度
                    orientation = 0;
                }
                else if( orientation > 80 &&orientation < 100 ) { //90度
                    orientation= 90;
                }
                else if( orientation > 170 &&orientation < 190 ) { //180度
                    orientation= 180;
                }
                else if( orientation > 260 &&orientation < 280  ) { //270度
                    orientation= 270;
                }
                else {
                    return;
                }
                TakePictureActivity.this.orientation = orientation;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
