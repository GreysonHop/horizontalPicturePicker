package me.example.greyson.horizontalpictureselector;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by Greyson on 2018/6/4.
 */

public class TestPictureSelectAct extends FragmentActivity implements PictureSelectPanel.OnSendClickListener {

    PictureSelectPanel panel;
    TextView pictureTV;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.act_test_picture_select);
        pictureTV = (TextView) findViewById(R.id.pictureTV);
        panel = new PictureSelectPanel(this, findViewById(R.id.pictureLayout));
        panel.setOnSendClickListener(this);

        Executors.newFixedThreadPool(100);
        Executors.newSingleThreadExecutor();
        Executors.newCachedThreadPool();
        Executors.newScheduledThreadPool(100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (panel != null) {
            panel.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSendImage(List<LocalMedia> list) {
        boolean isCompress = panel.isCompressMode();
        for (LocalMedia media : list) {
            if (isCompress) {
                Log.w("greyson", "send compress: " + media.getCompressPath());
            } else {
                Log.w("greyson", "send origin: " + media.getPath());
            }
        }
    }

    @Override
    public void onSendVideo(List<LocalMedia> list) {

    }

    public void onClick(View view) {
        if (pictureTV.isSelected()) {
            pictureTV.setSelected(false);
            panel.hide();
        } else {
            pictureTV.setSelected(true);
            panel.show();
        }
    }

}
