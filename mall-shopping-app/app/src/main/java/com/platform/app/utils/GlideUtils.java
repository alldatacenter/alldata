package com.platform.app.utils;


import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.platform.app.R;


/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/07
 *     desc   :对glide进行封装的工具类
 *     version: 2.0
 * </pre>
 */

public class GlideUtils {

    /**
     * 加载普通的图片
     *
     * @param context
     * @param url
     * @param iv
     */
    public static void load(Context context, String url, ImageView iv) {

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.company_logo)
                .error(R.drawable.default_head)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.NONE);

        Glide.with(context).load(url).apply(options).into(iv);
    }

    /**
     * 展示用户头像的Glide
     *
     * @param url
     * @param iv  最好去掉动画.不去动画,有可能(每次)出现第一次只实现展位图,第二次进入时是好图片
     *            如果用户没有传图像,直接加载默认的用户图像
     */
    public static void portrait(Context context, String url, ImageView iv) {

        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.company_logo)
                .error(R.drawable.default_head)
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.NONE);

        if (TextUtils.isEmpty(url)) {
            Glide.with(context).load(R.drawable.default_head).apply(options).into(iv);
        } else {
            Glide.with(context).load(url).apply(options).into(iv);
        }
    }

}
