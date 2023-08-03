/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.dashboard.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParamSignatureCalculator {
  public static final Logger LOG = LoggerFactory.getLogger(ParamSignatureCalculator.class);

  public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

  public static String getMD5(byte[] bytes) {
    char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    char[] str = new char[16 * 2];
    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
      md.update(bytes);
      byte[] tmp = md.digest();
      int k = 0;
      for (int i = 0; i < 16; i++) {
        byte byte0 = tmp[i];
        str[k++] = hexDigits[byte0 >>> 4 & 0xf];
        str[k++] = hexDigits[byte0 & 0xf];
      }
    } catch (Exception e) {
      LOG.error("failed", e);
    }
    return new String(str);
  }

  /**
   * To calculate md5
   *
   * @param value
   * @return
   */
  public static String getMD5(String value) {
    String result = "";
    try {
      result = getMD5(value.getBytes("UTF-8"));
    } catch (Exception e) {
      LOG.error("getRuntime MD5 Error!!", e);
    }
    return result;
  }

  /**
   * Gets an ascending KeyValue concatenation string based on the request argument pair.
   * Example：
   * params: name=&value=111&age=11&sex=1&high=180&nick=
   * Remove null and arrange in ascending order： age11high180sex1value111
   *
   * @param map
   * @return
   */
  public static String generateParamStringWithValueList(Map<String, List<String>> map) {
    Set<String> set = map.keySet();
    String[] keyArray = (String[]) set.toArray(new String[set.size()]);
    StringBuffer sb = new StringBuffer("");
    Arrays.sort(keyArray);
    String firstValue = "";
    for (int i = 0; i < keyArray.length; i++) {
      List<String> values = map.get(keyArray[i]);
      Collections.sort(values);

      if (values.size() >= 1) {
        if (!StringUtils.isBlank(values.get(0))) {
          try {
            firstValue = URLDecoder.decode(values.get(0), "utf-8");
          } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to caculate signature", e);
            return null;
          }
          sb.append(keyArray[i]).append(firstValue);
        }
      }
    }
    return sb.toString();
  }

  /**
   * Gets an ascending KeyValue concatenation string based on the request argument pair.
   * Example：
   * params: name=&value=111&age=11&sex=1&high=180&nick=
   * Remove null and arrange in ascending order： age11high180sex1value111
   *
   * @param map
   * @return
   */
  public static String generateParamStringWithValue(Map<String, String> map) {
    Set<String> set = map.keySet();
    String[] keyArray = (String[]) set.toArray(new String[set.size()]);
    StringBuffer sb = new StringBuffer("");
    Arrays.sort(keyArray);
    String firstValue = "";
    for (int i = 0; i < keyArray.length; i++) {
      String value = map.get(keyArray[i]);
      if (!StringUtils.isBlank(value)) {
        try {
          firstValue = URLDecoder.decode(value, "utf-8");
        } catch (UnsupportedEncodingException e) {
          LOG.error("Failed to caculate signature", e);
          return null;
        }
        sb.append(keyArray[i]).append(firstValue);
      }
    }
    return sb.toString();
  }



}
