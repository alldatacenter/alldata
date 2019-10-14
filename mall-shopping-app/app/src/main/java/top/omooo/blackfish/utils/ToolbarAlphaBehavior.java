package top.omooo.blackfish.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/3/31.
 */

public class ToolbarAlphaBehavior extends CoordinatorLayout.Behavior<Toolbar> {
    private static final String TAG = "ToolbarAlphaBehavior";
    private int offset = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    private Context mContext;

    public ToolbarAlphaBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull Toolbar child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return true;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull Toolbar child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        endOffset = child.getHeight();
        offset += dyConsumed;
        RelativeLayout relativeLayout = (RelativeLayout) child.getChildAt(0);
        ImageView imageLogo = (ImageView) relativeLayout.getChildAt(0);
        TextView textTitle = (TextView) relativeLayout.getChildAt(1);
        ImageView imageMsg = (ImageView) relativeLayout.getChildAt(2);

        // TODO: 2018/4/1 imageMsg从白变黑，而不是从有到无 
        if (offset <= startOffset) {  //alpha为0
            child.getBackground().setAlpha(0);
            Log.i(TAG, "onNestedScroll: " + textTitle.getText());
            textTitle.setAlpha(0);

        } else if (offset > startOffset && offset < endOffset) { //alpha为0到255
            imageLogo.setImageDrawable(mContext.getDrawable(R.drawable.icon_home_header_fish_logo));
            imageMsg.setImageDrawable(mContext.getDrawable(R.drawable.icon_home_header_msg_white));
            float precent = (float) (offset - startOffset) / endOffset;
            int alpha = Math.round(precent * 255);
            child.getBackground().setAlpha(alpha);

            textTitle.setVisibility(View.VISIBLE);
            textTitle.setAlpha(precent);

            imageLogo.getDrawable().setAlpha(1 - alpha);
            imageMsg.getDrawable().setAlpha(1-alpha);

        } else if (offset >= endOffset) {  //alpha为255
            child.getBackground().setAlpha(255);
            textTitle.setVisibility(View.VISIBLE);
            textTitle.setAlpha(1);
            imageMsg.setImageDrawable(mContext.getDrawable(R.drawable.icon_home_header_msg_black));
            imageMsg.setAlpha(1f);
        }
    }
}
