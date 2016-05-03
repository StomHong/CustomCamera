package com.stomhong.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 预览页面
 *
 * @author StomHong
 * @since 2016-4-23
 */
public class PreviewActivity extends Activity implements View.OnClickListener {

    private static final String TAG = PreviewActivity.class.getSimpleName();
    private LinearLayout mLinearLayout_Back;
    private ImageView mImageView_Preview;
    private String mPathName;
    private File mFile;
    private RadioButton mRadioButton_30per;
    private RadioButton mRadioButton_60per;
    private RadioButton mRadioButton_100per;

    private TextView mTextView_30per;
    private TextView mTextView_60per;
    private TextView mTextView_100per;
    private TextView mTextView_UsePicture;
    private Bitmap mBitmap;
    private int orientation;
    private byte[] data;
    private boolean isSavePictureFile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        initView();
        initData();
        initEvent();
        Log.i(TAG,"onCreate执行了");
    }

    private void initData() {
        //分两种情况：1.自定义相机 2.系统相机
        mRadioButton_30per.setChecked(true);
        mPathName = getIntent().getStringExtra("pathName");
        orientation = getIntent().getExtras().getInt("orientation");
        int flag = getIntent().getIntExtra("flag",0);
        if (flag == 304){
           orientation = 270;
        }
        //先旋转照片
        mBitmap = rotateImage(mPathName, orientation);
        //再添加水印
        mBitmap = addWatermarkToImage(mBitmap);
        //显示原图预览照
        mImageView_Preview.setImageBitmap(mBitmap);
        //最后压缩照片
        data = compressImage(mBitmap,30);
        //计算压缩图片的大小
        String imageSize = formatImageSize(data.length);
        mTextView_30per.setText(imageSize);
        mFile = new File(mPathName);


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart执行了");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageView_Preview.setImageBitmap(mBitmap);
        Log.i(TAG,"onResume执行了");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("bitmap",mBitmap);

        super.onSaveInstanceState(outState);

        Log.i(TAG,"onSaveInstanceState执行了");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,"onPause执行了");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop执行了");
        mBitmap.recycle();
        if (!isSavePictureFile){
            if (mFile != null){
                mFile.delete();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy执行了");
    }

    private void initEvent() {
        mImageView_Preview.setOnClickListener(this);
        mTextView_UsePicture.setOnClickListener(this);
        mLinearLayout_Back.setOnClickListener(this);
        mRadioButton_100per.setOnClickListener(this);
        mRadioButton_60per.setOnClickListener(this);
        mRadioButton_30per.setOnClickListener(this);
    }

    private void initView() {
        mLinearLayout_Back = (LinearLayout) findViewById(R.id.id_lay_back);
        mImageView_Preview = (ImageView) findViewById(R.id.id_iv_preview);

        mRadioButton_30per = (RadioButton) findViewById(R.id.id_rb_30per);
        mRadioButton_60per = (RadioButton) findViewById(R.id.id_rb_60per);
        mRadioButton_100per = (RadioButton) findViewById(R.id.id_rb_100per);

        mTextView_30per = (TextView) findViewById(R.id.id_tv_30per);
        mTextView_60per = (TextView) findViewById(R.id.id_tv_60per);
        mTextView_100per = (TextView) findViewById(R.id.id_tv_100per);
        mTextView_UsePicture = (TextView) findViewById(R.id.id_tv_use_picture);
    }

    @Override
    public void onClick(View v) {

        Intent intent = new Intent(this, MainActivity.class);
        String imageSize;

        switch (v.getId()) {

            case R.id.id_lay_back:
                mFile.delete();
                startActivity(intent);
                finish();
                break;

            case R.id.id_tv_use_picture:
                isSavePictureFile = true;
                //将原文件删除
                mFile.delete();
                //写入新的文件
                writeFileIntoSDCard(data, mPathName);
                intent.putExtra("pathName", mPathName);
                startActivity(intent);
                finish();
                break;

            case R.id.id_rb_30per:
                mRadioButton_30per.setChecked(true);
                data = compressImage(mBitmap, 30);
                imageSize = formatImageSize(data.length);
                mTextView_30per.setText(imageSize);
                mTextView_100per.setText("原图");
                mTextView_60per.setText("60%");
                break;

            case R.id.id_rb_60per:
                mRadioButton_60per.setChecked(true);
                data = compressImage(mBitmap, 60);
                imageSize = formatImageSize(data.length);
                mTextView_60per.setText(imageSize);
                mTextView_30per.setText("30%");
                mTextView_100per.setText("原图");
                break;

            case R.id.id_rb_100per:
                mRadioButton_100per.setChecked(true);
                data = compressImage(mBitmap, 100);
                imageSize = formatImageSize(data.length);
                mTextView_100per.setText(imageSize);
                mTextView_30per.setText("30%");
                mTextView_60per.setText("60%");
                break;

            default:
                break;
        }
    }

    /**
     * 计算图片大小
     *
     * @return 格式化的图片大小字符串
     */
    private String formatImageSize(int length) {

        String result = "";
        BigDecimal bd;
        double len = length;
        if (len > 0 && len < 1000000) {
            //以K为单位
            len = len / 1000;
            bd = new BigDecimal(len);
            bd = bd.setScale(2, BigDecimal.ROUND_CEILING);//四舍五入
            result = bd + "K";
        } else if (len >= 1000000) {
            //以M为单位
            len = len / 1000000;
            bd = new BigDecimal(len);
            bd = bd.setScale(2, BigDecimal.ROUND_CEILING);
            result = bd + "M";
        }
        return result;
    }

    /**
     * 压缩图片
     *
     * @param sBitmap 元bitmap
     * @param quality 压缩质量从0-100，100表示不压缩
     * @return bitmap Bitmap对象
     */
    private byte[] compressImage(Bitmap sBitmap, int quality) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    /**
     * 将照片旋转至90度
     *
     * @param pathName    文件路径
     * @param orientation 文件翻转的角度
     * @return 90度的bitmap
     */
    private Bitmap rotateImage(String pathName, int orientation) {

        Bitmap bitmap = BitmapFactory.decodeFile(pathName);
        Matrix matrix = new Matrix();
        matrix.postRotate((float) (orientation + 90));
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    /**
     * 给照片添加水印
     *
     * @param sBitmap 元Bitmap
     * @return 带有水印文字的bitmap
     */
    public Bitmap addWatermarkToImage(Bitmap sBitmap) {
        //这里他先将两张图片合并，然后再写上字
        //我的想法是先在阴影图片写上字，然后再合并
//        Bitmap shadow = BitmapFactory.decodeResource(getResources(), R.drawable.watermark).copy(Bitmap.Config.ARGB_8888,true);
//        Canvas canvas = new Canvas(shadow);
//        Paint textPaint = new Paint();
//        Log.d(TAG,sBitmap.getDensity()+"密度......................");
//        Log.d(TAG,sBitmap.getHeight()+"高度......................");
//        Log.d(TAG,sBitmap.getWidth()+"宽度......................");
//        textPaint.setColor(Color.WHITE);
//        //字体的大小和什么有关？和bitmap的density有关？还是和Bitmap的高度有关？
//        textPaint.setTextSize(30.0f);
//        textPaint.setTypeface(Typeface.DEFAULT);
//        textPaint.setDither(true);
//        textPaint.setAntiAlias(true);
//        textPaint.setFilterBitmap(true);
//
//        //测试机
////        canvas.drawText("商店名称", 5, shadow.getHeight()/10 + 20, textPaint);
////        canvas.drawText("31232321321312", 5,shadow.getHeight()/10 * 3 , textPaint);
////        canvas.drawText("very good", 5, shadow.getHeight()/10 * 4,textPaint);
////        canvas.drawText("2016-4-27", shadow.getHeight()/10 * 9, shadow.getHeight()/10 * 5, textPaint);
//        //我的手机
//        canvas.drawText("商店名称", 5, shadow.getHeight()/100 * 10, textPaint);
//        canvas.drawText("31232321321312", 5,shadow.getHeight()/100 * 15, textPaint);
//        canvas.drawText("very good", 5, shadow.getHeight()/100 * 20,textPaint);
//        canvas.drawText("2016-4-27", shadow.getHeight()/100 * 90, shadow.getHeight()/100 * 30, textPaint);
//        Canvas canvasTarget = new Canvas(sBitmap);
//        Paint photoPaint = new Paint();
//        photoPaint.setAlpha(100);
//        photoPaint.setDither(true);
//        canvasTarget.drawBitmap(shadow,0,sBitmap.getHeight() - sBitmap.getHeight()/10 * 2 ,photoPaint);


        Bitmap bitmap = sBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint photoPaint = new Paint();
        // 获取跟清晰的图像采样
        photoPaint.setDither(true);
        // 过滤一些
        photoPaint.setFilterBitmap(true);
        //水印照片目录
        Bitmap photo = BitmapFactory.decodeResource(getResources(), R.drawable.watermark);
        //透明参数
        photoPaint.setAlpha(100);
        // 在mBitmap的右下角画入水印
        canvas.drawBitmap(photo, 0, bitmap.getHeight() - 90 - 40, photoPaint);
        // 设置画笔
        Paint textPaint = new Paint();
        //设置是否抗锯齿
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(40.0f);
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        //绘制字符串
        canvas.drawText("商店名称", 5, bitmap.getHeight() - 65 - 30, textPaint);
        canvas.drawText("23423434234", 5, bitmap.getHeight() - 40 - 15, textPaint);
        canvas.drawText("许晴是我女神", 5, bitmap.getHeight() - 15, textPaint);
        canvas.drawText(getSystemTime(), bitmap.getWidth() / 10 * 7, bitmap.getHeight() - 15, textPaint);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        return bitmap;
    }

    /**
     * 将文件以I/O流的方式写入SDCard
     *
     * @param data     图片的数据
     * @param pathName 要写入的路径
     */
    private void writeFileIntoSDCard(byte[] data, String pathName) {

        File file = new File(pathName);
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
    }

    /**
     * 获取系统时间，返回特定的时间格式
     *
     * @return time 指定的时间格式
     */
    private String getSystemTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINA);
        Date curDate = new Date(System.currentTimeMillis());
        return sdf.format(curDate);
    }

}
