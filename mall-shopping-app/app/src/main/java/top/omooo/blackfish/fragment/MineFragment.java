package top.omooo.blackfish.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.SingleLayoutHelper;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.LinkedList;
import java.util.List;

import top.omooo.blackfish.LoginActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.GeneralVLayoutAdapter;

/**
 * Created by Omooo on 2018/2/25.
 */

public class MineFragment extends android.support.v4.app.Fragment {

    private Context mContext;

    private VirtualLayoutManager layoutManager;
    private RecyclerView.RecycledViewPool viewPool;
    private DelegateAdapter delegateAdapter;
    final List<DelegateAdapter.Adapter> adapters = new LinkedList<>();

    private RecyclerView mRecyclerView;

    public static MineFragment newInstance() {
        MineFragment fragment = new MineFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine_layout, null);
        mRecyclerView = view.findViewById(R.id.rv_fragment_mine_container);

        initView();
        loadPager();
        return view;
    }

    private void initView() {
        mContext = getActivity();

        layoutManager = new VirtualLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        viewPool = new RecyclerView.RecycledViewPool();
        mRecyclerView.setRecycledViewPool(viewPool);
        viewPool.setMaxRecycledViews(0, 20);

        delegateAdapter = new DelegateAdapter(layoutManager, false);
        mRecyclerView.setAdapter(delegateAdapter);

//        getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.home_header_yellow));
    }

    private void loadPager() {
        SingleLayoutHelper headerHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter headerAdapter = new GeneralVLayoutAdapter(getActivity(), headerHelper, 1){
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.mine_pager_header_layout, parent, false);
                SimpleDraweeView draweeView = view.findViewById(R.id.iv_mine_header_portrait);
                ImageView settingImage = view.findViewById(R.id.iv_mine_pager_setting);
                TextView textView = view.findViewById(R.id.tv_user_tel_number);
                draweeView.setImageURI(getUriFromDrawableRes(getActivity(), R.drawable.image_mine_pager_user));
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        sendCode();
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                    }
                });
                return new MainViewHolder(view);
            }
        };
        adapters.add(headerAdapter);
        delegateAdapter.setAdapters(adapters);
    }
    public Uri getUriFromDrawableRes(Context context, int id) {
        Resources resources = context.getResources();
        String path = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + resources.getResourcePackageName(id) + "/"
                + resources.getResourceTypeName(id) + "/"
                + resources.getResourceEntryName(id);
        return Uri.parse(path);
    }
}
