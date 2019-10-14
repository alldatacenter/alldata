package top.omooo.blackfish.fragment;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;

import butterknife.BindView;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.WebSettingsUtil;

/**
 * Created by Omooo on 2018/2/25.
 */

public class FinancialFragment extends NewBaseFragment {

    @BindView(R.id.webview)
    WebView mWebview;
    private Context mContext;

    public static FinancialFragment newInstance() {
        return new FinancialFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_financial_layout;
    }

    @Override
    public void initView() {
        mContext = getActivity();
        WebSettingsUtil.setSettings(mWebview);
        // TODO: 2018/4/12 无法加载该网页
        mWebview.loadUrl("http://omooo.top/");

        mWebview.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK&&mWebview.canGoBack()) {
                    mWebview.goBack();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void initData() {

    }

}
