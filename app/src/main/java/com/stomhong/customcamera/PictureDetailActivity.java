package com.stomhong.customcamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * 查看照片页面
 * @author StomHong
 * @since 2016-4-20
 */
public class PictureDetailActivity extends Activity {

    private ImageView mImageView_PictureDetail;
    private RelativeLayout mRelativeLayout_Back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_picture_detail);

        mImageView_PictureDetail = (ImageView) findViewById(R.id.id_iv_picture_detail);
        mRelativeLayout_Back = (RelativeLayout) findViewById(R.id.id_lay_back);
        mRelativeLayout_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.id_lay_back){
                    finish();
                }
            }
        });
        if (getIntent().getExtras() != null) {
            String pathName = getIntent().getStringExtra("pathName");
            Bitmap bitmap = BitmapFactory.decodeFile(pathName);
            mImageView_PictureDetail.setImageBitmap(bitmap);
        }

    }
}
