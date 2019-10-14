package top.omooo.blackfish;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.omooo.blackfish.utils.WebSettingsUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/4/12.
 */

public class BaseWebViewActivity extends FragmentActivity {

    private Context mContext;

    @BindView(R.id.iv_back)
    ImageView mIvBack;
    @BindView(R.id.tv_title)
    TextView mTvTitle;
    @BindView(R.id.iv_share)
    ImageView mIvShare;
    @BindView(R.id.webview)
    WebView mWebview;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_web_view_layout);
        ButterKnife.bind(this);

        mContext = BaseWebViewActivity.this;

        WebSettingsUtil.setSettings(mWebview);
        Intent intent = getIntent();
        mWebview.loadUrl(intent.getStringExtra("loadUrl"));
        mWebview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mTvTitle.setText("正在加载中......");
                } else {
                    //略略略
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                mTvTitle.setText(title);
            }
        });
    }

    @OnClick({R.id.iv_back, R.id.iv_share})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finish();
                overridePendingTransition(R.anim.activity_banner_left_in,R.anim.activity_banner_right_out);
                break;
            case R.id.iv_share:
                showBottomSheetDialog();
                break;
            default:
                break;
        }
    }

    private void showBottomSheetDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(mContext);
        View view = LayoutInflater.from(this).inflate(R.layout.view_share_bottom_sheet_dialog_layout, null);
        dialog.setContentView(view);
        ImageView imageView=view.findViewById(R.id.iv_close_dialog);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.iv_wechat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomToast.show(mContext, "分享至微信好友");
            }
        });
        view.findViewById(R.id.iv_wechat_friend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomToast.show(mContext, "分享至微信朋友圈");
            }
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            overridePendingTransition(R.anim.activity_banner_left_in,R.anim.activity_banner_right_out);
        }
        return true;
    }
}
