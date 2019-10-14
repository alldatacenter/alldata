package top.omooo.blackfish.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import top.omooo.blackfish.R;

/**
 * Created by Omooo on 2018/2/25.
 */

/**
 * 项目初始的Fragment基类
 */
public abstract class BaseFragment extends android.support.v4.app.Fragment implements View.OnClickListener{
    private boolean isVisible = false;
    private boolean isInitView = false;
    private boolean isFirstLoad = true;

    public View convertView;
    private SparseArray<View> mViews;

    public abstract int getLayoutId();

    public abstract void initViews();

    public abstract void initListener();

    public abstract void initData();

    public abstract void processClick(View view);

    @Override
    public void onClick(View v) {
        processClick(v);
    }

    /**
     * 实现Fragment的懒加载
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isVisible = true;
            lazyLoad();
        } else {
            isVisible = false;
        }
    }

    private void lazyLoad() {
        if (!isFirstLoad || !isVisible || !isInitView) {
            //如果不是第一次加载、不是可见的、不是初始化View，则不加载数据
            return;
        }
        //加载数据
        initListener();
        initData();
        isFirstLoad = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mViews = new SparseArray<>();
        convertView = inflater.inflate(getLayoutId(), container, false);
        initViews();

        isInitView = true;
        lazyLoad();
        return convertView;
    }

    public <E extends View> E findView(int viewId) {
        if (convertView != null) {
            E view = (E) mViews.get(viewId);
            if (view == null) {
                view = convertView.findViewById(viewId);
                mViews.put(viewId, view);
            }
            return view;
        }
        return null;
    }

    public <E extends View> void setOnClick(E convertView) {
        convertView.setOnClickListener(this);
    }

    public void skipActivity(Intent intent) {
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
    }

}
