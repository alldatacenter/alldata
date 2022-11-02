package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.deserializer.FieldTypeResolver;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class StringUtil {

    public static FieldTypeResolver FIELD_TYPE_RESOLVER = (object, fieldName) -> JSONObject.class;

    private static final String GLOBAL_PARAM_PREFIX = "Global.";

    /**
     * Trim 字符串辅助函数
     *
     * @param text   原始文本
     * @param trimBy Trim 字符串
     * @return
     */
    public static String trimStringByString(String text, String trimBy) {
        int beginIndex = 0;
        int endIndex = text.length();

        while (text.substring(beginIndex, endIndex).startsWith(trimBy)) {
            beginIndex += trimBy.length();
        }

        while (text.substring(beginIndex, endIndex).endsWith(trimBy)) {
            endIndex -= trimBy.length();
        }

        return text.substring(beginIndex, endIndex);
    }

    /**
     * 将指定的 name 增加 Global 变量前缀
     *
     * @param name 变量名称
     * @return 增加前缀后的名称
     */
    public static String globalParamName(String name) {
        return GLOBAL_PARAM_PREFIX + name;
    }

    public static Object recursiveDecodeObject(Object current) {
        if (current instanceof JSONObject) {
            for (Map.Entry<String, Object> entry : ((JSONObject) current).entrySet()) {
                ((JSONObject) current).put(entry.getKey(), recursiveDecodeObject(entry.getValue()));
            }
            return current;
        } else if (current instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) current).size(); i++) {
                ((JSONArray) current).set(i, recursiveDecodeObject(((JSONArray) current).get(i)));
            }
            return current;
        } else if (current instanceof String) {
            log.info("CURRENT_RECURSIVE_DECODE_OBJECT_STRING: {}", (String) current);
            if (StringUtils.isEmpty((String) current) ||
                StringUtils.isNumeric((String) current) ||
                Boolean.TRUE.equals(BooleanUtils.toBooleanObject((String) current)) ||
                Boolean.FALSE.equals(BooleanUtils.toBooleanObject((String) current))) {
                return current;
            }
            try {
                return JSON.parse((String) current);
            } catch (Exception e) {
                log.info("EXCEPTION_ON_DECODE_OBJECT: {}", ExceptionUtils.getStackTrace(e));
                return current;
            }
        }
        return current;
    }

    /**
     * split string to string list
     *
     * @param text
     * @param separator
     * @return
     */
    public static List<String> splitToList(String text, String separator) {
        if (StringUtils.isEmpty(text)) {
            return new ArrayList<>();
        }
        return Splitter.on(separator).trimResults().splitToList(text);

    }

    public static String joinToString(List<String> text, String separator) {
        if (text == null) {
            return StringUtils.EMPTY;
        }
        return String.join(separator, text);
    }

    public static String joinToString(List<String> text) {
        return joinToString(text, ",");
    }

    public static List<String> splitToList(String text) {
        return splitToList(text, ",");
    }

    public static List<String> splitToListIfEmptyWithNull(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return splitToList(text, ",");
    }

    public static String join(String... atomLabel) {
        return StringUtils.join(atomLabel, ",");
    }

    public static byte[] createChecksum(String filename) {
        try {
            InputStream fis = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            return complete.digest();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                String.format("cannot create checksum for file %s", filename), e);
        }
    }

    /**
     * 获取指定文件的 MD5
     *
     * @param filename 文件绝对路径
     * @return md5
     */
    public static String getMd5Checksum(String filename) {
        byte[] b = createChecksum(filename);
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    /**
     * 计算指定 content 对应的 md5 码
     *
     * @param content 要计算的字符串
     * @return
     */
    public static String md5sum(String content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(content));
            return String.format("%032x", new BigInteger(1, md5.digest()));
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "calc md5 failed|content=" + content);
        }
    }
}
