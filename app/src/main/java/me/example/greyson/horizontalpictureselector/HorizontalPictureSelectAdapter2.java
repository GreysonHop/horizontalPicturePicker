package me.example.greyson.horizontalpictureselector;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.DateUtils;
import com.luck.picture.lib.tools.VoiceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Greyson on 2018/11/15.
 * 把外部RecyclerView对Adapter的操作，即选中与未选中的UI变化的逻辑放到Adapter里面<br>
 * 并且图片选中状态的变化是通过改变item的CheckBox（部分组件）来进行的
 */
public class HorizontalPictureSelectAdapter2 extends RecyclerView.Adapter<HorizontalPictureSelectAdapter2.ViewHolder> {

    private Context context;
    private RecyclerView recyclerView;

    private List<LocalMedia> images = new ArrayList<LocalMedia>();
    private List<LocalMedia> selectImages = new ArrayList<LocalMedia>();
    private int maxSelectNum = 9;

    public HorizontalPictureSelectAdapter2(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picture_select, parent, false);
        return new HorizontalPictureSelectAdapter2.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final ViewHolder contentHolder = holder;

        final LocalMedia localMedia = images.get(position);
        localMedia.position = contentHolder.getAdapterPosition();
        final String path = localMedia.getPath();
        String pictureType = localMedia.getPictureType();

        contentHolder.checkTV.setText("");
        LocalMedia selectedMedia = findOneInSelectList(localMedia);
        if (selectedMedia != null) {
            localMedia.setNum(selectedMedia.getNum());
            selectedMedia.setPosition(localMedia.getPosition());
            selectImage(contentHolder, localMedia.getNum(), false);
        } else {
            selectImage(contentHolder, 0, false);
        }

        final int mediaMimeType = PictureMimeType.isPictureType(pictureType);
        if (mediaMimeType == PictureConfig.TYPE_VIDEO) {
            contentHolder.videoIV.setVisibility(View.VISIBLE);
            contentHolder.durationTV.setVisibility(View.VISIBLE);
        } else {
            contentHolder.videoIV.setVisibility(View.INVISIBLE);
            contentHolder.durationTV.setVisibility(View.INVISIBLE);
        }

        long duration = localMedia.getDuration();
        contentHolder.durationTV.setText(DateUtils.timeParse(duration));
        Glide.with(context)
                .load(path)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .into(contentHolder.pictureIV);

        contentHolder.checkLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如原图路径不存在或者路径存在但文件不存在
                if (!new File(path).exists()) {
                    Toast.makeText(context, "文件出错！", Toast.LENGTH_SHORT).show();
                    return;
                }
                changeCheckboxState(contentHolder, localMedia);
            }
        });

        contentHolder.contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w("greyson", "click: num=" + localMedia.getNum()
                        + " - position=" + localMedia.getPosition() + " - path=" + localMedia.getPath());
                // 如原图路径不存在或者路径存在但文件不存在
                if (!new File(path).exists()) {
                    Toast.makeText(context, "文件出错！", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean eqResult =
                        mediaMimeType == PictureConfig.TYPE_IMAGE
                                || mediaMimeType == PictureConfig.TYPE_VIDEO;
                if (eqResult) {
                    if (imageSelectChangeListener != null) {
                        imageSelectChangeListener.onPictureClick(localMedia, position);
                    }
                } else {
                    changeCheckboxState(contentHolder, localMedia);
                }
            }
        });
    }

    /**
     * 改变（反转）图片选中状态
     *
     * @param contentHolder
     * @param image
     */
    private void changeCheckboxState(ViewHolder contentHolder, LocalMedia image) {
        boolean isChecked = contentHolder.checkTV.isSelected();
        String pictureType = selectImages.size() > 0 ? selectImages.get(0).getPictureType() : "";
        if (!TextUtils.isEmpty(pictureType)) {
            boolean toEqual = PictureMimeType.mimeToEqual(pictureType, image.getPictureType());
            if (!toEqual) {
                Toast.makeText(context, context.getString(R.string.picture_rule), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (selectImages.size() >= maxSelectNum && !isChecked) {
            boolean eqImg = pictureType.startsWith(PictureConfig.IMAGE);
            String str = eqImg ? context.getString(com.luck.picture.lib.R.string.picture_message_max_num, maxSelectNum)
                    : context.getString(com.luck.picture.lib.R.string.picture_message_video_max_num, maxSelectNum);
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isChecked) {
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(image.getPath())) {
                    selectImages.remove(media);
                    updateSelectPosition();
//                    disZoom(contentHolder.pictureIV);
                    break;
                }
            }
        } else {
            // 如果是单选，则清空已选中的并刷新列表(作单一选择)
            /*if (selectMode == PictureConfig.SINGLE) {
                singleRadioMediaImage();
            }*/
            selectImages.add(image);
            image.setNum(selectImages.size());
            VoiceUtils.playVoice(context, false);
//            zoom(contentHolder.pictureIV);
        }

        selectImage(contentHolder, isChecked ? 0 : selectImages.size(), true);

        if (imageSelectChangeListener != null) {
            imageSelectChangeListener.onChange(selectImages);
        }
    }

    /**
     * 选中的图片,打勾和加上阴影
     *
     * @param holder
     * @param checkNum 被选中图片的顺序, 大小0才有效，其它数值默认未选中
     * @param isAnim
     */
    public void selectImage(ViewHolder holder, int checkNum, boolean isAnim) {
        if (checkNum > 0) {
           /* if (isAnim) {
                if (animation != null) {
                    holder.checkTV.startAnimation(animation);
                }
            }*/
            holder.pictureIV.setColorFilter(context.getResources().getColor
                    (R.color.image_overlay_true), PorterDuff.Mode.SRC_ATOP);
            holder.checkTV.setSelected(true);
            holder.checkTV.setText(String.valueOf(checkNum));

        } else {

            holder.pictureIV.setColorFilter(context.getResources().getColor
                    (R.color.image_overlay_false), PorterDuff.Mode.SRC_ATOP);
            holder.checkTV.setSelected(false);
            holder.checkTV.setText("");

        }
    }

    public boolean isSelected(LocalMedia image) {
        return findOneInSelectList(image) != null;
    }

    public LocalMedia findOneInSelectList(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return media;
            }
        }
        return null;
    }

    /**
     * 更新选择的顺序
     */
    private void updateSelectPosition() {
        int size = selectImages.size();
        for (int index = 0; index < size; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);

            if (recyclerView != null) {
                ViewHolder viewHolder = (ViewHolder) recyclerView.findViewHolderForLayoutPosition(media.position);
                if (viewHolder != null) {
                    viewHolder.checkTV.setText(String.valueOf(media.getNum()));
                }

            }
        }
    }

    public void updateSelectImages(List<LocalMedia> images) {
        this.selectImages = images;
        notifyDataSetChanged();
    }

    public void updateImagesData(List<LocalMedia> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    public List<LocalMedia> getSelectedImages() {
        if (selectImages == null) {
            selectImages = new ArrayList<>();
        }
        return selectImages;
    }

    public List<LocalMedia> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }

    @Override
    public int getItemCount() {
        return images == null ? 0 : images.size();
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (images != null && images.size() > holder.getAdapterPosition()) {
            LocalMedia localMedia = images.get(holder.getAdapterPosition());
            selectImage(holder, isSelected(localMedia) ? localMedia.getNum() : 0, false);
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    private OnImageSelectChangeListener imageSelectChangeListener;

    public void setImageSelectChangeListener(OnImageSelectChangeListener imageSelectChangeListener) {
        this.imageSelectChangeListener = imageSelectChangeListener;
    }

    public interface OnImageSelectChangeListener {
        /**
         * 已选Media回调
         *
         * @param selectImages
         */
        void onChange(List<LocalMedia> selectImages);

        /**
         * 图片预览回调
         *
         * @param media
         * @param position
         */
        void onPictureClick(LocalMedia media, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pictureIV;
        View checkLL;
        TextView checkTV;
        ImageView videoIV;
        TextView durationTV;
        View contentView;

        public ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView;
            pictureIV = (ImageView) itemView.findViewById(R.id.pictureIV);
            checkLL = itemView.findViewById(R.id.checkLL);
            checkTV = (TextView) itemView.findViewById(R.id.checkTV);
            videoIV = (ImageView) itemView.findViewById(R.id.videoIV);
            durationTV = (TextView) itemView.findViewById(R.id.durationTV);
        }
    }
}
