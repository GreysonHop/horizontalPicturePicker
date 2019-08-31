package me.example.greyson.horizontalpictureselector;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/**
 * Created by Greyson on 2018/9/24.
 */
public class SuitedImageView extends AppCompatImageView {

    private int resize_base_on = -1;
    private final int RESIZE_BASE_ON_WIDTH = 0;
    private final int RESIZE_BASE_ON_HEIGHT = 1;

    public SuitedImageView(Context context) {
        this(context, null);
    }

    public SuitedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SuitedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SuitedImageView);
        resize_base_on = ta.getInt(R.styleable.SuitedImageView_resize_base_on, -1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (resize_base_on == -1 || getDrawable() == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        Drawable drawable = getDrawable();
        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();
        //图片宽高比
        float dScale = (float) dWidth / (float) dHeight;
//        float finalScale = dScale /*> 1.5 ? 1.5f : dScale*/;

        int widthSize;
        int heightSize;
        widthSize = MeasureSpec.getSize(widthMeasureSpec);
        heightSize = MeasureSpec.getSize(heightMeasureSpec);
        System.out.println("greyson's maxHeight = " + getMaxHeight() + " - " + getMaxWidth());
        switch (resize_base_on) {
            case RESIZE_BASE_ON_WIDTH:
                heightSize = (int) (widthSize / dScale);
                heightSize = getMaxHeight() > heightSize ? heightSize : getMaxHeight();
                break;

            case RESIZE_BASE_ON_HEIGHT:
                widthSize = (int) (heightSize * dScale);
                widthSize = getMaxWidth() > widthSize ? widthSize : getMaxWidth();
                break;
        }

//        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    public class ImageViewUtil {
        public void matchAll(Context context, AppCompatImageView imageView) {
            int width, height;//ImageView调整后的宽高
            //获取屏幕宽高
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(metrics);
            int sWidth = metrics.widthPixels;
            int sHeight = metrics.heightPixels;
            //获取图片宽高
            Drawable drawable = imageView.getDrawable();
            int dWidth = drawable.getIntrinsicWidth();
            int dHeight = drawable.getIntrinsicHeight();

            //屏幕宽高比,一定要先把其中一个转为float
            float sScale = (float) sWidth / sHeight;
            //图片宽高比
            float dScale = (float) dWidth / dHeight;
        /*
        缩放比
        如果sScale>dScale，表示在高相等的情况下，控屏幕比较宽，这时候要适应高度，缩放比就是两则的高之比，图片宽度用缩放比计算
        如果sScale<dScale，表示在高相等的情况下，图片比较宽，这时候要适应宽度，缩放比就是两则的宽之比，图片高度用缩放比计算
         */
            float scale = 1.0f;
            if (sScale > dScale) {
                scale = (float) dHeight / sHeight;
                height = sHeight;//图片高度就是屏幕高度
                width = (int) (dWidth * scale);//按照缩放比算出图片缩放后的宽度
            } else if (sScale < dScale) {
                scale = (float) dWidth / sWidth;
                width = sWidth;
                height = (int) (dHeight / scale);//这里用除
            } else {
                //最后两者刚好比例相同，其实可以不用写，刚好充满
                width = sWidth;
                height = sHeight;
            }
            //重设ImageView宽高
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
            imageView.setLayoutParams(params);
            //这样就获得了一个既适应屏幕有适应内部图片的ImageView，不用再纠结该给ImageView设定什么尺寸合适了
        }
    }

}
