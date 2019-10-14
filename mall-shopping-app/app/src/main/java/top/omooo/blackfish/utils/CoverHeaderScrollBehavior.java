package top.omooo.blackfish.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by SSC on 2018/4/13.
 */

public class CoverHeaderScrollBehavior extends CoordinatorLayout.Behavior<View> {

    private static final String TAG = "CoverScrollBehavior";

    public CoverHeaderScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (null != params && params.height == CoordinatorLayout.LayoutParams.MATCH_PARENT) {
            child.layout(0, 0, parent.getWidth(), parent.getHeight());
            child.setTranslationY(getHeaderHeight());
            return true;
        }
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        Log.i(TAG, "onNestedPreScroll: " + dy);
//        ImageView imageView = (ImageView) coordinatorLayout.getChildAt(0);
        //在这个方法里面只处理向上滑动
        if (dy < 0) {
            return;
        }
        float transY = child.getTranslationY() - dy;
        if (transY > 0) {
            child.setTranslationY(transY);
            consumed[1] = dy;
        }
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        Log.i(TAG, "onNestedScroll: " + dyUnconsumed);
        //在这个方法里只处理向下滑动
        if (dyUnconsumed > 0) {
            return;
        }

        float transY = child.getTranslationY() - dyUnconsumed;
        if (transY > 0 && transY < getHeaderHeight()) {
            child.setTranslationY(transY);
        }
    }

    private int getHeaderHeight() {
        return 600;
    }

}
