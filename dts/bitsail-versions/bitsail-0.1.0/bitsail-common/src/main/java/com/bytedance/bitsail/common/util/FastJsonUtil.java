/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Slf4j
public class FastJsonUtil {
  public static Object parse(String json) {
    return JSON.parse(json, Feature.DisableSpecialKeyDetect);
  }

  public static <T> T parseObject(String json, Class<T> clazz) {
    return JSON.parseObject(json, clazz, Feature.IgnoreAutoType);
  }

  public static JSONObject parseObject(String json) {
    return JSON.parseObject(json, Feature.DisableSpecialKeyDetect);
  }

  public static List<SerializerFeature> parseSerializerFeaturesFromConfig(String featureString) {
    if (StringUtils.isEmpty(featureString)) {
      return new ArrayList<>();
    }

    val featureList = featureString.replaceAll("\\s", "").split(",");
    return Arrays.stream(featureList).map(SerializerFeature::valueOf).collect(Collectors.toList());
  }
}
