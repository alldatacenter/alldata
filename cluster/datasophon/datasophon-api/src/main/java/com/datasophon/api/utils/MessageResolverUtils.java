package com.datasophon.api.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Arrays;

/**
 * desc： 获取i18n资源文件
 */
public class MessageResolverUtils {

    @Autowired
    private static MessageSource messageSource = SpringUtil.getBean(MessageSource.class);

    public MessageResolverUtils() {
    }

    /**
     * 根据 messageKey 获取国际化消息 委托给 spring messageSource
     *
     * @param code 消息key
     * @return 解析后的国际化
     */
    public static String getMessage(Object code) {
        return messageSource.getMessage(code.toString(), null, code.toString(), LocaleContextHolder.getLocale());
    }

    /**
     * 根据 messageKey 和参数 获取消息 委托给 spring messageSource
     *
     * @param code        消息key
     * @param messageArgs 参数
     * @return 解析后的国际化
     */
    public static String getMessages(Object code, Object... messageArgs) {
        Object[] objs = Arrays.stream(messageArgs).map(MessageResolverUtils::getMessage).toArray();
        String message = messageSource.getMessage(code.toString(), objs, code.toString(), LocaleContextHolder.getLocale());
        return message;
    }
}
