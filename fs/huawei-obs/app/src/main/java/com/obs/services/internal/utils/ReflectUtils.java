/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.ServiceException;

public class ReflectUtils {
    private static final ILogger ILOG = LoggerBuilder.getLogger(ReflectUtils.class);
    
    private static Class<?> androidBase64Class;

    private static Class<?> jdkBase64EncoderClass;

    private static Class<?> jdkBase64DecoderClass;

    private static Object jdkNewEncoder;

    private static Object jdkNewDecoder;

    static {
        try {
            androidBase64Class = Class.forName("android.util.Base64");
        } catch (ClassNotFoundException e) {
            if (ILOG.isTraceEnabled()) {
                ILOG.trace("class not found.", e);
            }
        }

        try {
            Class<?> base64 = Class.forName("java.util.Base64");
            jdkNewEncoder = base64.getMethod("getEncoder").invoke(null);
            jdkNewDecoder = base64.getMethod("getDecoder").invoke(null);
        } catch (ClassNotFoundException e) {
            if (ILOG.isTraceEnabled()) {
                ILOG.trace("class not found exception.", e);
            }
        } catch (IllegalAccessException e) {
            ILOG.warn("illegal access exception.", e);
        } catch (IllegalArgumentException e) {
            ILOG.warn("illegal argument exception.", e);
        } catch (InvocationTargetException e) {
            ILOG.warn("invocation target exception.", e);
        } catch (NoSuchMethodException e) {
            ILOG.warn("nosuch method exception.", e);
        } catch (SecurityException e) {
            ILOG.warn("security exception.", e);
        }

        try {
            jdkBase64EncoderClass = Class.forName("sun.misc.BASE64Encoder");
        } catch (ClassNotFoundException e) {
            if (ILOG.isTraceEnabled()) {
                ILOG.trace("class not found exception.", e);
            }
        }

        try {
            jdkBase64DecoderClass = Class.forName("sun.misc.BASE64Decoder");
        } catch (ClassNotFoundException e) {
            if (ILOG.isTraceEnabled()) {
                ILOG.trace("class not found exception.", e);
            }
        }
    }

    public static String toBase64(byte[] data) {
        if (androidBase64Class != null) {
            try {
                Method m = androidBase64Class.getMethod("encode", byte[].class, int.class);
                return new String((byte[]) m.invoke(null, data, 2), Charset.defaultCharset());
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }

        if (jdkNewEncoder != null) {
            try {
                Method m = jdkNewEncoder.getClass().getMethod("encode", byte[].class);
                return new String((byte[]) m.invoke(jdkNewEncoder, data), StandardCharsets.UTF_8).replaceAll("\\s", "");
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }

        if (jdkBase64EncoderClass != null) {
            try {
                Method m = jdkBase64EncoderClass.getMethod("encode", byte[].class);
                return ((String) m.invoke(jdkBase64EncoderClass.getConstructor().newInstance(), data)).replaceAll("\\s",
                        "");
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }

        throw new ServiceException("Failed to find a base64 encoder");
    }

    public static byte[] fromBase64(String b64Data) throws UnsupportedEncodingException {
        if (androidBase64Class != null) {
            try {
                Method m = androidBase64Class.getMethod("decode", byte[].class, int.class);
                return (byte[]) m.invoke(null, b64Data.getBytes(StandardCharsets.UTF_8), 2);
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }

        if (jdkNewDecoder != null) {
            try {
                Method m = jdkNewDecoder.getClass().getMethod("decode", byte[].class);
                return (byte[]) m.invoke(jdkNewDecoder, b64Data.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }

        if (jdkBase64DecoderClass != null) {
            try {
                Method m = jdkBase64DecoderClass.getMethod("decodeBuffer", String.class);
                return (byte[]) m.invoke(jdkBase64DecoderClass.getConstructor().newInstance(), b64Data);
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }
        throw new ServiceException("Failed to find a base64 decoder");
    }
}
