package me.example.greyson.horizontalpictureselector;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.widget.PreviewViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by Greyson on 2018/6/5.
 */
public class PicturePreviewActivity extends FragmentActivity implements View.OnClickListener, PreviewFragmentAdapter.OnCallBackActivity {

    public static final int REQUEST_PREVIEW_AND_CHOOSE = 111;

    private Context mContext;
    private ImageView picture_left_back;
    private TextView tv_img_num, tv_title, tv_ok;
    private PreviewViewPager viewPager;
    private LinearLayout id_ll_ok;
    private TextView check;
    private LinearLayout ll_check;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();

    private int maxSelectNum;
    private int position;
    private PreviewFragmentAdapter adapter;

    private boolean refresh;
    private int index;

    boolean numComplete = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().register(this);
        }
        this.mContext = this;
        setContentView(R.layout.act_picture_preview);

        picture_left_back = (ImageView) findViewById(R.id.picture_left_back);
        viewPager = (PreviewViewPager) findViewById(R.id.preview_pager);
        ll_check = (LinearLayout) findViewById(R.id.ll_check);
        id_ll_ok = (LinearLayout) findViewById(R.id.id_ll_ok);
        check = (TextView) findViewById(R.id.check);
        picture_left_back.setOnClickListener(this);
        tv_ok = (TextView) findViewById(R.id.tv_ok);
        id_ll_ok.setOnClickListener(this);
        tv_img_num = (TextView) findViewById(R.id.tv_img_num);
        tv_title = (TextView) findViewById(R.id.picture_title);

        maxSelectNum = getIntent().getIntExtra("maxSelectNum", 3);

        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        tv_ok.setText(numComplete ? getString(R.string.picture_done_front_num,
                0, maxSelectNum) : getString(R.string.picture_please_select));
        tv_img_num.setSelected(true);

        List<LocalMedia> list = (List<LocalMedia>) getIntent().getSerializableExtra(PictureConfig.EXTRA_SELECT_LIST);
        if (list != null) {
            selectImages = list;
        }

        images = (List<LocalMedia>) getIntent().getSerializableExtra("image_list");
//        images = ImagesObservable.getInstance().readLocalMedias();
        initViewPageAdapterData();


        initEvent();
    }

    private void initEvent() {
        ll_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (images != null && images.size() > 0) {
                    LocalMedia image = images.get(viewPager.getCurrentItem());
                    String pictureType = selectImages.size() > 0 ?
                            selectImages.get(0).getPictureType() : "";
                    if (!TextUtils.isEmpty(pictureType)) {
                        boolean toEqual = PictureMimeType.
                                mimeToEqual(pictureType, image.getPictureType());
                        if (!toEqual) {
                            Toast.makeText(mContext, getString(R.string.picture_rule), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // 刷新图片列表中图片状态
                    boolean isChecked;
                    if (!check.isSelected()) {
                        isChecked = true;
                        check.setSelected(true);
//                        check.startAnimation(animation);
                    } else {
                        isChecked = false;
                        check.setSelected(false);
                    }
                    if (selectImages.size() >= maxSelectNum && isChecked) {
                        Toast.makeText(mContext, getString(R.string.picture_message_max_num, maxSelectNum), Toast.LENGTH_SHORT).show();
                        check.setSelected(false);
                        return;
                    }
                    if (isChecked) {
//                        VoiceUtils.playVoice(mContext, config.openClickSound);

                        selectImages.add(image);
                        image.setNum(selectImages.size());
                        check.setText(String.valueOf(image.getNum()));
                    } else {
                        for (LocalMedia media : selectImages) {
                            if (media.getPath().equals(image.getPath())) {
                                selectImages.remove(media);
                                subSelectPosition();
                                notifyCheckChanged(media);
                                break;
                            }
                        }
                    }
                    onSelectNumChange(true);
                }
            }
        });

        viewPager.setOnPageChangeListener(/*);
        viewPager.addOnPageChangeListener(*/new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                isPreviewEggs(config.previewEggs, position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int i) {
                position = i;
                tv_title.setText(position + 1 + "/" + images.size());
                LocalMedia media = images.get(position);
                index = media.getPosition();
//                if (!config.previewEggs) {
                check.setText(media.getNum() + "");
                notifyCheckChanged(media);
                onImageChecked(position);
//                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    /**
     * 初始化ViewPage数据
     */
    private void initViewPageAdapterData() {
        tv_title.setText(position + 1 + "/" + images.size());
        adapter = new PreviewFragmentAdapter(images, this, this);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        onSelectNumChange(false);
        onImageChecked(position);
        if (images.size() > 0) {
            LocalMedia media = images.get(position);
            index = media.getPosition();
            tv_img_num.setSelected(true);
            check.setText(media.getNum() + "");
            notifyCheckChanged(media);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && images.size() > 0) {
            LocalMedia media = images.get(position);
            check.setSelected(isSelected(media));
        } else {
            check.setSelected(false);
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */
    public void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = selectImages.size() != 0;
        if (enable) {
            tv_ok.setSelected(true);
            id_ll_ok.setEnabled(true);
            if (numComplete) {
                tv_ok.setText(getString(R.string.picture_done_front_num, selectImages.size(), maxSelectNum));
            } else {
                if (refresh) {
//                    tv_img_num.startAnimation(animation);
                }
                tv_img_num.setVisibility(View.VISIBLE);
                tv_img_num.setText(String.valueOf(selectImages.size()));
                tv_ok.setText(getString(R.string.picture_completed));
            }
        } else {
            id_ll_ok.setEnabled(false);
            tv_ok.setSelected(false);
            if (numComplete) {
                tv_ok.setText(getString(R.string.picture_done_front_num, 0, maxSelectNum));
            } else {
                tv_img_num.setVisibility(View.INVISIBLE);
                tv_ok.setText(getString(R.string.picture_please_select));
            }
        }
        updateSelector(refresh);
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    private void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            EventEntity obj = new EventEntity(PictureConfig.UPDATE_FLAG, selectImages, index);
            RxBus.getDefault().post(obj);
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(LocalMedia imageBean) {
        check.setText("");
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(imageBean.getPath())) {
                imageBean.setNum(media.getNum());
                check.setText(String.valueOf(imageBean.getNum()));
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == picture_left_back) {
            onBackPressed();
            return;
        }

        if (v == id_ll_ok) {
// 如果设置了图片最小选择数量，则判断是否满足条件
            int size = selectImages.size();
            LocalMedia image = selectImages.size() > 0 ? selectImages.get(0) : null;
            String pictureType = image != null ? image.getPictureType() : "";
                if (size < 1) {
                    boolean eqImg = pictureType.startsWith(PictureConfig.IMAGE);
                    String str = eqImg ? getString(R.string.picture_min_img_num, 1)
                            : getString(R.string.picture_min_video_num, 1);
                    Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
                    return;
                }
            /*if (config.enableCrop && pictureType.startsWith(PictureConfig.IMAGE)) {
                if (config.selectionMode == PictureConfig.SINGLE) {
                    originalPath = image.getPath();
                    startCrop(originalPath);
                } else {
                    // 是图片和选择压缩并且是多张，调用批量压缩
                    ArrayList<String> cuts = new ArrayList<>();
                    for (LocalMedia media : selectImages) {
                        cuts.add(media.getPath());
                    }
                    startCrop(cuts);
                }
            } else {*/
                setResult(selectImages);
//            }
        }
    }

    protected void setResult(List<LocalMedia> images) {
        /*RxBus.getDefault().post(new EventEntity(PictureConfig.PREVIEW_DATA_FLAG, images));
        // 如果开启了压缩，先不关闭此页面，PictureImageGridActivity压缩完在通知关闭
//        if (!config.isCompress) {
            onBackPressed();
        /*} else {
            showPleaseDialog();
        }*/

        Intent intent = PictureSelector.putIntentResult(images);
        setResult(RESULT_OK, intent);
        closeActivity();
    }

    @Override
    public void onBackPressed() {
        closeActivity();
    }

    /**
     * Close Activity
     */
    protected void closeActivity() {
        finish();
        /*if (config.camera) {
            overridePendingTransition(0, R.anim.fade_out);
        } else {*/
        overridePendingTransition(0, R.anim.a3);
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onActivityBackPressed() {

    }
}
