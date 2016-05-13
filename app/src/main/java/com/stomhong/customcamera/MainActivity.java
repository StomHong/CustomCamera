package com.stomhong.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * <p>有两种方式：Camera和CameraDevice</p>
 * Camera在API21被废止，推荐使用后者来操作
 *
 * @author StomHong
 * @since 2016-4-20
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Youxiao/"+"p"+getSystemTime()+".jpg";
    private Button mButton_OpenCustomCamera;
    private Button mButton_OpenSystemCamera;
    private Button mButton_OpenAlbum;
    private ImageView mImageView_Picture;
    private String mPathName;
    private static final String IMAGE_TEYPE = "image/*";
    private static final int SYSTEM_CAMERA_REQUEST_CODE = 302;
    private static final int IMAGE_REQUEST_CODE = 303;
    private static final String ACTION_STOMHONG_CUSTOM_CAMERA = "com.stomhong.custom.camera";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initEvent();
    }


    private void initEvent() {
        mButton_OpenCustomCamera.setOnClickListener(this);
        mButton_OpenAlbum.setOnClickListener(this);
        mButton_OpenSystemCamera.setOnClickListener(this);
        mImageView_Picture.setOnClickListener(this);
    }

    private void initData() {

        if (getIntent().getExtras() != null) {
            mPathName = getIntent().getStringExtra("pathName");
            Bitmap bitmap = BitmapFactory.decodeFile(mPathName);
            mImageView_Picture.setImageBitmap(bitmap);
        }
    }

    private void initView() {
        mButton_OpenCustomCamera = (Button) findViewById(R.id.id_btn_open_custom_camera);
        mButton_OpenSystemCamera = (Button) findViewById(R.id.id_btn_open_system_camera);
        mButton_OpenAlbum = (Button) findViewById(R.id.id_btn_open_album);
        mImageView_Picture = (ImageView) findViewById(R.id.id_iv_picture);
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent();

        switch (v.getId()) {

            case R.id.id_btn_open_custom_camera:
                intent.setAction(ACTION_STOMHONG_CUSTOM_CAMERA);
                startActivity(intent);
                break;

            case R.id.id_btn_open_system_camera:
               // 指定开启系统相机的Action
               intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
               intent.addCategory(Intent.CATEGORY_DEFAULT);
               //根据文件地址创建文件
               File file = new File(FILE_PATH);
               //把文件地址转换成Uri格式
               Uri uri = Uri.fromFile(file);
               //设置系统相机拍摄照片完成后图片文件的存放地址
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, SYSTEM_CAMERA_REQUEST_CODE);
                break;

            case R.id.id_btn_open_album:
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(IMAGE_TEYPE);
                //根据版本号不同使用不同的Action
                if (Build.VERSION.SDK_INT < 19) {
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                } else {
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                }
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
                break;

            case R.id.id_iv_picture:
                if (mPathName != null){
                    intent.setClass(this,PictureDetailActivity.class);
                    intent.putExtra("pathName",mPathName);
                    startActivity(intent);
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            switch (requestCode) {

                case 302:
                    //获取系统相机返回的数据
                    Intent intent = new Intent();
                    intent.setClass(this,PreviewActivity.class);
                    intent.putExtra("pathName",FILE_PATH);
                    intent.putExtra("flag",304);
                    startActivity(intent);
                    break;

                case 303:
                    //获取相册返回的数据
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        Bitmap bitmap = decodeSampledBitmapFromUri(uri,600,600);
                        mImageView_Picture.setImageBitmap(bitmap);
                    }
                    break;
            }
        }
    }

    /**
     * 计算取样率
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 从输入流中将图片解析成bitmap
     *
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public  Bitmap decodeSampledBitmapFromUri(Uri uri,int reqWidth, int reqHeight) {
        InputStream is = null;
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BitmapFactory.decodeStream(is, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        try {
            is = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(is, null, options);
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
}
