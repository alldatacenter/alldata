package com.alibaba.tesla.authproxy.util;

import com.alibaba.tesla.authproxy.model.mapper.AppExtMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.model.AppExtDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 验证相关工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class AuthUtil {

    @Autowired
    private AppExtMapper appExtMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeslaUserService teslaUserService;

    private static String getPasswdHash(String username, String password)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date today = Calendar.getInstance().getTime();
        String todayStr = df.format(today);
        String rawHash = String.format("%s%s%s", username, todayStr, password);
        byte[] bytesOfMessage = rawHash.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] theDigest = md.digest(bytesOfMessage);
        return convertHashBytes2Str(theDigest);
    }

    private static String getNextPasswdHash(String username, String password)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        String todayStr = df.format(calendar.getTime());
        String rawHash = String.format("%s%s%s", username, todayStr, password);
        byte[] bytesOfMessage = rawHash.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] theDigest = md.digest(bytesOfMessage);
        return convertHashBytes2Str(theDigest);
    }

    private static String getPrePasswdHash(String username, String password)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        String todayStr = df.format(calendar.getTime());
        String rawHash = String.format("%s%s%s", username, todayStr, password);
        byte[] bytesOfMessage = rawHash.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] theDigest = md.digest(bytesOfMessage);
        return convertHashBytes2Str(theDigest);
    }

    private static String convertHashBytes2Str(byte[] bytes) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char str[] = new char[16 * 2];
        int k = 0;
        for (int i = 0; i < 16; i++) {
            byte byte0 = bytes[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }

    /**
     * 检查是否携带第三方 APP Header，如果携带则检查合法性
     *
     * @param request HTTP 请求
     * @return 合法则返回 true
     */
    public UserDO getExtAppUser(HttpServletRequest request) {
        String authApp = request.getHeader("x-auth-app");
        String authKey = request.getHeader("x-auth-key");
        String authUser = request.getHeader("x-auth-user");
        String authPassword = request.getHeader("x-auth-passwd");
        // fakePassword 为了兼容曾经的老版本的错误 Header 头名称
        String authFakePassword = request.getHeader("x-auth-password");
        if (!StringUtil.isEmpty(authFakePassword)) {
            authPassword = authFakePassword;
        }
        return getExtAppUser(authApp, authKey, authUser, authPassword);
    }

    /**
     * 检查第三方应用 Header 是否合法
     *
     * @param authApp      第三方应用名
     * @param authKey      应用 Key
     * @param authUser     关联用户
     * @param authPassword 关联用户 Secret Key
     * @return 如果合法则返回 true
     */
    public UserDO getExtAppUser(String authApp, String authKey, String authUser, String authPassword) {
        if (StringUtil.isEmpty(authApp) ||
                StringUtil.isEmpty(authKey) ||
                StringUtil.isEmpty(authUser) ||
                StringUtil.isEmpty(authPassword)) {
            return null;
        }
        String commonLog = String.format("app=%s, key=%s, user=%s, password=%s",
                authApp, authKey, authUser, authPassword);

        // 校验 authApp 和 authKey 两项
        AppExtDO appExt = appExtMapper.getByName(authApp);
        if (null == appExt) {
            log.info("No ext app found. {}", commonLog);
            return null;
        }
        if (!authKey.equals(appExt.getExtAppKey())) {
            log.info("Invalid ext app key. {}", commonLog);
            return null;
        }

        // 校验 authUser 和 authPassword 两项
        UserDO userDo = teslaUserService.getUserByLoginName(authUser);
        if (null == userDo) {
            log.info("Invalid x-auth-user provided, cannot find it. {}", commonLog);
            return null;
        }
        String secretKey = userDo.getSecretKey();
        if (null == secretKey || secretKey.length() == 0) {
            log.info("Invalid x-auth-user provided, no secret key set. {}", commonLog);
            return null;
        }
        String compareKey;
        try {
            compareKey = getPasswdHash(authUser, secretKey);
            if (!compareKey.equals(authPassword)
                    && !getNextPasswdHash(authUser, secretKey).equals(authPassword)
                    && !getPrePasswdHash(authUser, secretKey).equals(authPassword)) {
                log.info("Invalid x-auth-user provided, password check failed. {}", commonLog);
                return null;
            }
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            log.warn("Get password hash failed. {}, message={}", commonLog, e.getMessage());
            return null;
        }
        return userDo;
    }

}
