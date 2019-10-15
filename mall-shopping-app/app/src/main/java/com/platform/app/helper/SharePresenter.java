package com.platform.app.helper;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.utils.AppExistUtils;
import com.platform.app.utils.ToastUtils;

import com.platform.app.share.OnekeyShare;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;


/**
 * <pre>
 *     author : wulinhao
 *     e-mail : 2572694660@qq.com
 *     time   : 2019/02/10
 *     desc   : 社会化分享的封装
 * </pre>
 */


public class SharePresenter implements View.OnClickListener {

    private volatile static SharePresenter sGetShareInstance;

    private SharePresenter() {
    }

    public static SharePresenter getInstance() {

        if (sGetShareInstance == null) {
            synchronized (SharePresenter.class) {
                if (sGetShareInstance == null) {
                    sGetShareInstance = new SharePresenter();
                }
            }
        }
        return sGetShareInstance;
    }


    private static Dialog dialog;

    private String   platformType;   //分享到哪个平台
    private int      shareType;      //分享哪种类型(比如链接  视频  音频等等)
    private Activity mActivity;
    private String   title;
    private String   description;
    private String   id;    //分享链接的唯一id


    /**
     * 分享对话框，默认底部弹出
     */
    public void showShareDialogOnBottom(int shareType, Activity mActivity
            , String title, String description, String id) {

        this.shareType = shareType;
        this.mActivity = mActivity;
        this.title = title;
        this.description = description;
        this.id = id;


        dialog = new Dialog(mActivity, R.style.ActionSheetDialogStyle);
        View shareView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_main_share_bottom, null);
        ImageView close = (ImageView) shareView.findViewById(R.id.iv_dismiss);

        shareView.findViewById(R.id.weixin).setOnClickListener(this);
        shareView.findViewById(R.id.wxcircle).setOnClickListener(this);
        shareView.findViewById(R.id.sina).setOnClickListener(this);
        shareView.findViewById(R.id.qq).setOnClickListener(this);
        shareView.findViewById(R.id.qzone).setOnClickListener(this);


        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        //将布局设置给Dialog
        dialog.setContentView(shareView);
        //获取当前Activity所在的窗体
        Window dialogWindow = dialog.getWindow();
        //设置Dialog从窗体底部弹出
        dialogWindow.setGravity(Gravity.BOTTOM);
        //获得窗体的属性
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        //将属性设置给窗体
        dialogWindow.setAttributes(lp);
        dialog.show();

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.weixin:
                boolean wx = AppExistUtils.isWeixinAvilible(mActivity);
                if (wx) {
                    share(Wechat.NAME);
                } else {
                    ToastUtils.showSafeToast(MallShoppingApp.sContext,"未安装微信,无法分享");
                    dialog.dismiss();
                }
                break;
            case R.id.wxcircle:
                boolean wx1 = AppExistUtils.isWeixinAvilible(mActivity);
                if (wx1) {
                    share(WechatMoments.NAME);
                } else {
                    ToastUtils.showSafeToast(MallShoppingApp.sContext,"未安装微信,无法分享");
                    dialog.dismiss();
                }

                break;
            case R.id.sina:
                boolean wb = AppExistUtils.isWeiboAvailable(mActivity);
                if (wb) {
                    share(SinaWeibo.NAME);
                } else {
                    ToastUtils.showSafeToast(MallShoppingApp.sContext,"未安装微博,无法分享");
                    dialog.dismiss();
                }
                break;
            case R.id.qq:
                boolean qq = AppExistUtils.isQQClientAvailable(mActivity);
                if (qq) {
                    share(QQ.NAME);
                } else {
                    ToastUtils.showSafeToast(MallShoppingApp.sContext,"未安装QQ,无法分享");
                    dialog.dismiss();
                }
                break;
            case R.id.qzone:
                boolean qq1 = AppExistUtils.isQQClientAvailable(mActivity);
                if (qq1) {
                    share(QZone.NAME);
                } else {
                    ToastUtils.showSafeToast(MallShoppingApp.sContext,"未安装QQ,无法分享");
                    dialog.dismiss();
                }
                break;
        }
    }

    /**
     * 分享
     */
    private void share(String platform) {

        dialog.dismiss();

        OnekeyShare oks = new OnekeyShare();
        //指定分享的平台，如果为空，还是会调用九宫格的平台列表界面
        if (platform != null) {
            oks.setPlatform(platform);
        }

        //关闭sso授权
        oks.disableSSOWhenAuthorize();
        oks.setTitle(title);
        oks.setTitleUrl("http://www.baidu.com");
        oks.setText(description);
        oks.setUrl("http://sharesdk.cn");
        oks.setSite("来自商城App的分享");
        oks.setSiteUrl("http://www.baidu.com");

        // 启动分享GUI
        oks.show(mActivity);

    }
}
