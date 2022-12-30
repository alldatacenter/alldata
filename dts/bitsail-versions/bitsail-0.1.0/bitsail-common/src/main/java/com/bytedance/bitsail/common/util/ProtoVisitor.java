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

import com.google.common.collect.Maps;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 2020/5/29.
 */
public class ProtoVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(ProtoVisitor.class);

  public static Object getPathValue(DynamicMessage originMessage,
                                    FieldPathUtils.PathInfo pathInfo) {
    return getPathValue(originMessage, pathInfo, FeatureContext.defaultContext());
  }

  public static Object getPathValue(DynamicMessage originMessage,
                                    FieldPathUtils.PathInfo pathInfo,
                                    FeatureContext ctx) {
    // if value is null will return null current.
    if (Objects.isNull(pathInfo) || Objects.isNull(originMessage)) {
      return null;
    }

    Object pathMessage = originMessage;
    Descriptors.FieldDescriptor pathDescriptor = null;

    for (FieldPathUtils.PathInfo nextPath = pathInfo; Objects.nonNull(nextPath); nextPath = nextPath.getNestPathInfo()) {
      if (Objects.isNull(pathMessage)) {
        return null;
      }
      Pair<Descriptors.FieldDescriptor, Object> pair = getDescriptorAndMessageByName(pathMessage, nextPath);
      pathDescriptor = pair.getKey();
      pathMessage = pair.getValue();

      if (Objects.isNull(pathDescriptor)) {
        return null;
      }
      pathMessage = getPathMessage(pathDescriptor, (DynamicMessage) pathMessage, nextPath, ctx);
    }

    if (Objects.isNull(pathMessage)) {
      return null;
    }

    if (Objects.isNull(pathDescriptor)) {
      throw new IllegalArgumentException(String.format("Path info: %s can't find descriptor type.",
          JsonSerializer.serialize(pathInfo)));
    }

    return ProtoUtils.getProtoValue(pathMessage, pathDescriptor, ctx);
  }

  /**
   * Protobuf repeated message stored as list<DynamicMessage>, and every dynamic message contain key field
   * and value field, which key field's value stored user properties key, value field's value stored
   * user properties value.
   */
  private static Pair<Descriptors.FieldDescriptor, Object> getDescriptorAndMessageByName(Object pathValue, FieldPathUtils.PathInfo pathInfo) {
    Descriptors.FieldDescriptor targetDescriptor = null;
    Object targetMessage = null;

    if (pathValue instanceof DynamicMessage) {
      targetDescriptor = ((DynamicMessage) pathValue)
          .getDescriptorForType()
          .findFieldByName(pathInfo.getName());
      targetMessage = pathValue;
    } else if (pathValue instanceof List) {
      Map<String, DynamicMessage> mapPathValue = Maps.newHashMap();
      for (DynamicMessage message : (List<DynamicMessage>) pathValue) {
        mapPathValue.put(message.getField(message.getDescriptorForType()
            .findFieldByName(ProtoUtils.PROTO_MAP_TYPE_KEY)).toString(), message);
      }
      targetMessage = mapPathValue.get(pathInfo.getName());
      targetDescriptor = mapPathValue.get(pathInfo.getName())
          .getDescriptorForType()
          .findFieldByName(ProtoUtils.PROTO_MAP_TYPE_VALUE);
    }
    return ImmutablePair.of(targetDescriptor, targetMessage);
  }

  /**
   * Find path message field from raw message.
   */
  public static Object getPathMessage(Descriptors.FieldDescriptor fieldDescriptor,
                                      DynamicMessage message,
                                      FieldPathUtils.PathInfo pathInfo,
                                      FeatureContext ctx) {
    if (FieldPathUtils.PathType.ARRAY.equals(pathInfo.getPathType())) {
      int repeatedFieldCount = message.getRepeatedFieldCount(fieldDescriptor);
      if (repeatedFieldCount > pathInfo.getIndex()) {
        return ProtoUtils.getArrayField(message, fieldDescriptor, pathInfo.getIndex());
      }
      return null;
    } else {
      return ProtoUtils.getField(message, fieldDescriptor, ctx);
    }
  }

  public static ProtoVisitor.FeatureContext genFeatureContext(Map<String, String> features) {
    ProtoVisitor.FeatureContext context = ProtoVisitor.FeatureContext.defaultContext();
    if (MapUtils.isEmpty(features)) {
      return context;
    }
    for (String key : features.keySet()) {
      ProtoVisitor.Feature feature = ProtoVisitor.Feature.transformFeature(key);
      if (Objects.nonNull(feature)) {
        LOG.info("configuration feature = {} set value = {}.", feature, features.get(key));
        context.configureFeature(feature, Boolean.parseBoolean(features.get(key)));
      }
    }
    return context;
  }

  public enum Feature implements Serializable {
    USE_DEFAULT_VALUE(true),

    BINARY_STRING_AS_STRING(true);

    private static Map<String, Feature> FEATURES = Arrays.stream(Feature.values())
        .collect(Collectors.toMap(feature -> feature.name().toLowerCase(), Function.identity()));
    private boolean defaultState;

    Feature(boolean defaultState) {
      this.defaultState = defaultState;
    }

    public static Feature transformFeature(String feature) {
      return FEATURES.get(feature);
    }
  }

  public static class FeatureContext implements Serializable {
    private Map<Feature, Boolean> features;

    private FeatureContext() {
      this.features = Maps.newHashMap();
    }

    public static FeatureContext defaultContext() {
      return new FeatureContext();
    }

    public void enableFeature(Feature feature) {
      configureFeature(feature, true);
    }

    public void disableFeature(Feature feature) {
      configureFeature(feature, false);
    }

    public void configureFeature(Feature feature, boolean configured) {
      features.put(feature, configured);
    }

    public boolean isEnabled(Feature feature) {
      return features.containsKey(feature) ? features.get(feature)
          : feature.defaultState;
    }
  }
}
