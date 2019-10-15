package com.platform.app.utils;

import android.annotation.SuppressLint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author : wulinhao
 * e-mail : 2572694660@qq.com
 * time   : 2017\9\22 0022
 * desc   : 字符串操作工具包
 */
@SuppressLint("SimpleDateFormat")
public class StringUtils {

    /**
     * 大陆手机号码十一位数，匹配格式：前三位固定格式+后8位任意数
     * 此方法中前三位格式有：
     * 13+任意数
     * 15+除4的任意数
     * 18+除1和4的任意数
     * 17+除9的任意数
     * 147
     */
    public static boolean isMobileNum(String mobiles) {
        String regExp = "13\\d{9}|14[579]\\d{8}|15[0123456789]\\d{8}|17[01235678]\\d{8" +
                "}|18\\d{9}";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(mobiles);
        return m.matches();
    }


    /**
     * 设置密码的长度
     * @param pwd
     * @return
     */
    public static boolean isPwdStrong(String pwd) {
        if (pwd == null || pwd.length() < 6) {
            return false;
        } else {
            return true;
        }
    }

}
