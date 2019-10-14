package top.omooo.blackfish.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by SSC on 2018/4/10.
 */

/**
 * 写初始化写烦了，添加ButterKnife支持
 */
public abstract class NewBaseFragment extends android.support.v4.app.Fragment {
    private boolean isVisible = false;
    private boolean isInitView = false;
    private boolean isFirstLoad = true;

    private Unbinder mUnbinder;
    public View convertView;

    public abstract int getLayoutId();

    public abstract void initView();

    public abstract void initData();


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
        initData();
        isFirstLoad = false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getLayoutId() != 0) {
            convertView = inflater.inflate(getLayoutId(), container, false);
            mUnbinder = ButterKnife.bind(this, convertView);
            initView();
            isInitView = true;
            lazyLoad();
        }
        return convertView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
