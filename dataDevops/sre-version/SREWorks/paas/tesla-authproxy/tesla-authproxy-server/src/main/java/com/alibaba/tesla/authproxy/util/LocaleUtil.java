package com.alibaba.tesla.authproxy.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Service
public class LocaleUtil {

    @Autowired
    private MessageSource messageSource;

    /**
     * 获取资源文件中的消息
     * @param id 对应 message 中的资源 ID
     */
    public String msg(String id) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(id, null, locale);
        } catch (NoSuchMessageException e) {
            return id;
        }
    }

    /**
     * 获取资源文件中的消息，并使用 MessageFormat 进行格式化
     * @param id 对应 message 中的资源 ID
     */
    public String msg(String id, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            String message = messageSource.getMessage(id, null, locale);
            return MessageFormat.format(message, args);
        } catch (NoSuchMessageException e) {
            return id;
        }
    }

    /**
     * 根据原始 ID 获取转换为 HEX 格式的国际化资源文件
     * @param rawId 原始 ID
     */
    public String hexmsg(String rawId) {
        String id = String.format("%x", new BigInteger(1, rawId.getBytes()));
        try {
            return msg(id);
        } catch (NoSuchMessageException e) {
            return rawId;
        }
    }

    /**
     * 根据原始 ID 获取转换为 HEX 格式的国际化资源文件， 并使用 MessageFormat 进行格式化
     * @param rawId 原始 ID
     */
    public String hexmsg(String rawId, Object... args) {
        String id = String.format("%x", new BigInteger(1, rawId.getBytes()));
        try {
            return msg(id, args);
        } catch (NoSuchMessageException e) {
            return rawId;
        }
    }

}