package me.example.greyson.horizontalpictureselector;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.compress.CompressConfig;
import com.luck.picture.lib.compress.CompressImageOptions;
import com.luck.picture.lib.compress.CompressInterface;
import com.luck.picture.lib.compress.LubanOptions;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.model.LocalMediaLoader;
import com.luck.picture.lib.permissions.RxPermissions;
import com.luck.picture.lib.tools.DoubleUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Greyson on 2018/6/6.
 */
public class PictureSelectPanel implements View.OnClickListener, HorizontalPictureSelectAdapter2.OnImageSelectChangeListener {

    private FragmentActivity activity;
    private View pictureLayout;

    private int maxSelectNum = 9;
    private HorizontalPictureSelectAdapter2 adapter;
    private LocalMediaLoader mediaLoader;
    protected List<LocalMedia> selectionMedias = new ArrayList<>();
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMediaFolder> foldersList = new ArrayList<>();

    private RxPermissions rxPermissions;

    private TextView albumTV;
    private TextView originPictureTV;//原图选择按钮
    private TextView sendPictureTV;

    public PictureSelectPanel(FragmentActivity activity, View rootView) {
        if (rootView != null) {
            this.activity = activity;
            pictureLayout = rootView.findViewById(R.id.pictureLayout);
            initView(pictureLayout);
            initEvent();
        }
    }

    private void initView(View view) {
        albumTV = (TextView) view.findViewById(R.id.albumTV);
        originPictureTV = (TextView) view.findViewById(R.id.originPictureTV);
        sendPictureTV = (TextView) view.findViewById(R.id.sendPictureTV);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.pictureRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));

        adapter = new HorizontalPictureSelectAdapter2(activity);
        adapter.updateImagesData(new ArrayList<LocalMedia>());
        adapter.setImageSelectChangeListener(this);
        recyclerView.setAdapter(adapter);

    }

    private void initEvent() {
        rxPermissions = new RxPermissions(activity);
        mediaLoader = new LocalMediaLoader(activity, PictureMimeType.ofAll(), false, 20000);//20秒以内的短视频

        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            readLocalMedia();
                        } else {
                            Toast.makeText(activity, "please open", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

        albumTV.setOnClickListener(this);
        originPictureTV.setOnClickListener(this);
        sendPictureTV.setOnClickListener(this);
    }

    /**
     * get LocalMedia s
     */
    protected void readLocalMedia() {
        mediaLoader.loadAllMedia(new LocalMediaLoader.LocalMediaLoadListener() {
            @Override
            public void loadComplete(List<LocalMediaFolder> folders) {
                if (folders.size() > 0) {
                    foldersList = folders;
                    LocalMediaFolder folder = folders.get(0);
                    folder.setChecked(true);
                    List<LocalMedia> localImg = folder.getImages();
                    // 这里解决有些机型会出现拍照完，相册列表不及时刷新问题
                    // 因为onActivityResult里手动添加拍照后的照片，
                    // 如果查询出来的图片大于或等于当前adapter集合的图片则取更新后的，否则就取本地的
                    if (localImg.size() >= images.size()) {
                        images = localImg;
//                        folderWindow.bindFolder(folders);
                    }
                }
                if (adapter != null) {
                    if (images == null) {
                        images = new ArrayList<>();
                    }
                    adapter.updateImagesData(images);
                    /*tv_empty.setVisibility(images.size() > 0
                            ? View.INVISIBLE : View.VISIBLE);*/
                }
//                mHandler.sendEmptyMessage(DISMISS_DIALOG);
            }
        });
    }

    /**
     * 预览的Activity返回后，请回调该接口，同步数据
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
            switch (requestCode) {
                case PicturePreviewActivity.REQUEST_PREVIEW_AND_CHOOSE:
                    // 预览中的图片选择结果回调
                    // 例如 LocalMedia 里面返回三种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true
                    // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true

                    for (LocalMedia media : selectList) {
                        Log.i("从预览中回来，选中的图片：", media.getPath() + " - " + media.getCompressPath());
                    }
                    changeImageNumber(selectList);
                    adapter.updateSelectImages(selectList);
                    adapter.notifyDataSetChanged();
                    break;

                case PictureConfig.CHOOSE_REQUEST:

                    // 图片选择结果回调
                    // 如果裁剪并压缩了，已取压缩路径为准，因为是先裁剪后压缩的
                    for (LocalMedia media : selectList) {
                        Log.i("从相册回来，直接发送：", media.getPath() + " - " + media.getCompressPath());
                    }
                    String pictureType = selectList.get(0).getPictureType();
                    if (PictureMimeType.isPictureType(pictureType) == PictureConfig.TYPE_IMAGE) {
                        onSendClickListener.onSendImage(selectList);
                    } else {
                        onSendClickListener.onSendVideo(selectList);
                    }
                    /*adapter.setList(selectList);
                    adapter.notifyDataSetChanged();*/
                    break;
            }
        }
    }

    @Override
    public void onChange(List<LocalMedia> selectImages) {
        changeImageNumber(selectImages);
    }

    /**
     * change image selector state
     *
     * @param selectImages
     */
    public void changeImageNumber(List<LocalMedia> selectImages) {
        boolean enable = selectImages.size() != 0;
        if (enable) {
            sendPictureTV.setEnabled(true);
            sendPictureTV.setText(activity.getString(R.string.text_send_picture_with_num, selectImages.size()));
        } else {
            sendPictureTV.setEnabled(false);
            sendPictureTV.setText(activity.getString(R.string.text_send_picture));

            originPictureTV.setSelected(false);
        }
    }

    @Override
    public void onPictureClick(LocalMedia media, int position) {
        startPreview(adapter.getImages(), position);
    }

    /**
     * preview image and video
     *
     * @param previewImages
     * @param position
     */
    public void startPreview(List<LocalMedia> previewImages, int position) {
        if (previewImages.size() <= position) {
            Toast.makeText(activity, "data mistake", Toast.LENGTH_SHORT).show();
            return;
        }
        LocalMedia media = previewImages.get(position);
        String pictureType = media.getPictureType();
        Bundle bundle = new Bundle();
        List<LocalMedia> result = new ArrayList<>();
        int mediaType = PictureMimeType.isPictureType(pictureType);
        switch (mediaType) {
            case PictureConfig.TYPE_IMAGE:
                // image
                List<LocalMedia> selectedImages = adapter.getSelectedImages();
//                ImagesObservable.getInstance().saveLocalMedia(previewImages);//这个会跟原框架冲突！
                bundle.putSerializable("image_list", (Serializable) previewImages);
                bundle.putSerializable(PictureConfig.EXTRA_SELECT_LIST, (Serializable) selectedImages);
                bundle.putInt(PictureConfig.EXTRA_POSITION, position);
                bundle.putInt("maxSelectNum", maxSelectNum);

                if (!DoubleUtils.isFastDoubleClick()) {
                    Intent intent = new Intent();
                    intent.setClass(activity, PicturePreviewActivity.class);
                    intent.putExtras(bundle);
                    activity.startActivityForResult(intent, PicturePreviewActivity.REQUEST_PREVIEW_AND_CHOOSE);
                }
//                startActivity(, bundle, UCropMulti.REQUEST_MULTI_CROP);

                activity.overridePendingTransition(R.anim.a5, 0);
                break;
            case PictureConfig.TYPE_VIDEO:
                // video
                /*if (config.selectionMode == PictureConfig.SINGLE) {
                    result.add(media);
                    onResult(result);//把结果setResult(Bundle)
                } else {*/
                bundle.putString("video_path", media.getPath());
                if (!DoubleUtils.isFastDoubleClick()) {
                    Intent intent = new Intent();
                    intent.setClass(activity, PictureVideoPlayActivity.class);
                    intent.putExtras(bundle);
                    activity.startActivity(intent);
                }
//                    startActivity(PictureVideoPlayActivity.class, bundle);
//                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == albumTV) {
            PictureSelector.create(activity)
                    .openGallery(PictureMimeType.ofAll())// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                    .theme(R.style.picture_my_style)
                    .maxSelectNum(maxSelectNum)// 最大图片选择数量
                    .minSelectNum(1)// 最小选择数量
                    .imageSpanCount(4)// 每行显示个数
                    .selectionMode(PictureConfig.MULTIPLE)// 多选 or 单选
                    .previewImage(true)// 是否可预览图片
                    .previewVideo(true)// 是否可预览视频
                    .isCamera(false)// 是否显示拍照按钮
                    //.imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
                    //.setOutputCameraPath("/CustomPath")// 自定义拍照保存路径
                    .enableCrop(false)// 是否裁剪
                    .compress(!originPictureTV.isSelected())// 是否压缩
                    //.sizeMultiplier(0.5f)// glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
                    .glideOverride(160, 160)// glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
                    .selectionMedia(adapter == null ? selectionMedias : adapter.getSelectedImages())// 是否传入已选图片
//                        .videoMaxSecond(15)
//                        .videoMinSecond(10)
                    //.previewEggs(false)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
                    .videoSecond(20)//显示多少秒以内的视频or音频也可适用
                    .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
            return;
        }

        if (v == originPictureTV) {
            if (adapter.getSelectedImages().size() > 0) {
                List<LocalMedia> list = adapter.getSelectedImages();
                LocalMedia localMedia = list.get(0);//如何获取图片的大小？
                originPictureTV.setSelected(!originPictureTV.isSelected());
            }
            return;
        }

        if (v == sendPictureTV) {
            if (onSendClickListener == null) {
                return;
            }

            List<LocalMedia> selectedList = adapter.getSelectedImages();
            if (selectedList != null && selectedList.size() > 0) {
                String pictureType = selectedList.get(0).getPictureType();
                if (PictureMimeType.isPictureType(pictureType) == PictureConfig.TYPE_IMAGE) {
                    if (originPictureTV.isSelected()) {
                        onSendClickListener.onSendImage(selectedList);
                    } else {
                        compressImage(selectedList);
                    }
                } else {
                    onSendClickListener.onSendVideo(selectedList);
                }

                //清空选项
                adapter.updateSelectImages(new ArrayList<LocalMedia>());
                sendPictureTV.setEnabled(false);
                sendPictureTV.setText(activity.getString(R.string.text_send_picture));
            }

        }
    }

    private void compressImage(final List<LocalMedia> result) {
        CompressConfig compress_config = CompressConfig.ofDefaultConfig();
        LubanOptions option = (new LubanOptions.Builder())/*.setMaxHeight(this.compressHeight)
                .setMaxWidth(this.compressWidth).setMaxSize(this.compressMaxKB)
                .setGrade(this.compressGrade)*/.create();
        compress_config = CompressConfig.ofLuban(option);

        CompressImageOptions.compress(activity, compress_config, result, new CompressInterface.CompressListener() {
            public void onCompressSuccess(List<LocalMedia> images) {
                onSendClickListener.onSendImage(images);
            }

            public void onCompressError(List<LocalMedia> images, String msg) {
                onSendClickListener.onSendImage(result);
            }
        }).compress();
    }

    public boolean isCompressMode() {
        return !originPictureTV.isSelected();
    }

    private OnSendClickListener onSendClickListener;

    public void setOnSendClickListener(OnSendClickListener onSendClickListener) {
        this.onSendClickListener = onSendClickListener;
    }

    public interface OnSendClickListener {
        void onSendImage(List<LocalMedia> list);
        void onSendVideo(List<LocalMedia> list);
    }

    public void show() {
        if (pictureLayout != null && !isShowing()) {
            pictureLayout.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (pictureLayout != null && isShowing()) {
            pictureLayout.setVisibility(View.GONE);
        }
    }

    public boolean isShowing() {
        return pictureLayout.getVisibility() == View.VISIBLE;
    }
}
