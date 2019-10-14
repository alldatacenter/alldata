package top.omooo.blackfish.utils;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by SSC on 2018/4/12.
 */

public class WebSettingsUtil {
    public static void setSettings(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.canGoBack();
        //自适应屏幕
        webSettings.setUseWideViewPort(true);   //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true);  //缩放至屏幕的大小

        //缩放操作
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);   //设置内置的缩放控件
        webSettings.setDisplayZoomControls(false);  //隐藏原生的缩放控件

        //优先使用缓存
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        //设置可访问文件
        webSettings.setAllowFileAccess(true);
        //支持通过JS打开新的窗口
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        //支持自动加载图片
        webSettings.setLoadsImagesAutomatically(true);
        //设置编码格式
        webSettings.setDefaultTextEncodingName("utf-8");

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //打开网页时不调用系统浏览器，而是在本地webview中显示
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                switch (error.getErrorCode()) {
                    case ERROR_FILE_NOT_FOUND:

                        break;
                    default:
                        break;
                }
            }
        });
    }

}
