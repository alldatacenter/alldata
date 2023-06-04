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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.util.List;
import java.util.Map;

/**
 * Created 2020/5/29.
 */
public class ProtoUtils {
  static final String PROTO_MAP_TYPE_KEY = "key";
  static final String PROTO_MAP_TYPE_VALUE = "value";

  public static Object getProtoValue(Object message,
                                     Descriptors.FieldDescriptor fieldDescriptor,
                                     ProtoVisitor.FeatureContext ctx) {
    boolean repeated = fieldDescriptor.isRepeated();
    if (repeated && message instanceof List) {
      if (fieldDescriptor.isMapField()) {
        JSONObject messageMapValue = new JSONObject();
        for (Object nestDynamicMessage : (List) message) {
          messageMapValue.putAll((Map) getMessageValue(nestDynamicMessage, fieldDescriptor, ctx));
        }
        return messageMapValue;
      } else {
        List<Object> messageArrayValue = new JSONArray();
        for (Object nestDynamicMessage : (List) message) {
          messageArrayValue.add(getMessageValue(nestDynamicMessage, fieldDescriptor, ctx));
        }
        return messageArrayValue;
      }
    }
    return getMessageValue(message, fieldDescriptor, ctx);
  }

  /**
   * Protobuf map field's message type still is {@link Descriptors.FieldDescriptor.JavaType#MESSAGE}, so when we
   * handle {@link Descriptors.FieldDescriptor.JavaType#MESSAGE} need check it is map field descriptor or not.
   */
  private static Object getMessageValue(Object message,
                                        Descriptors.FieldDescriptor fieldDescriptor,
                                        ProtoVisitor.FeatureContext ctx) {
    Descriptors.FieldDescriptor.JavaType javaType = fieldDescriptor.getJavaType();
    switch (javaType) {
      case MESSAGE:
        JSONObject messageValue = new JSONObject();

        if (!fieldDescriptor.isMapField()) {
          Map<Descriptors.FieldDescriptor, Object> fields = ((DynamicMessage) message).getAllFields();
          for (Descriptors.FieldDescriptor nestFieldDescriptor : fields.keySet()) {
            messageValue.put(
                nestFieldDescriptor.getName(),
                getProtoValue(fields.get(nestFieldDescriptor), nestFieldDescriptor, ctx));
          }
        } else {
          Descriptors.FieldDescriptor keyFieldDescriptor = ((DynamicMessage) message)
              .getDescriptorForType()
              .findFieldByName(PROTO_MAP_TYPE_KEY);
          Descriptors.FieldDescriptor valueFieldDescriptor = ((DynamicMessage) message)
              .getDescriptorForType()
              .findFieldByName(PROTO_MAP_TYPE_VALUE);
          messageValue.put(
              getProtoValue(getField((DynamicMessage) message, keyFieldDescriptor, ctx), keyFieldDescriptor, ctx).toString(),
              getProtoValue(getField((DynamicMessage) message, valueFieldDescriptor, ctx), valueFieldDescriptor, ctx));
        }
        return messageValue;
      case ENUM:
        return ((Descriptors.EnumValueDescriptor) message).getName();
      case BYTE_STRING:
        return ((ByteString) message).toByteArray();
      default:
        return message;
    }
  }

  public static Object getField(DynamicMessage dynamicMessage,
                                Descriptors.FieldDescriptor fieldDescriptor,
                                ProtoVisitor.FeatureContext ctx) {

    if (ctx.isEnabled(ProtoVisitor.Feature.USE_DEFAULT_VALUE)
        || fieldDescriptor.isRepeated()
        || dynamicMessage.hasField(fieldDescriptor)) {
      return dynamicMessage.getField(fieldDescriptor);
    }
    return null;
  }

  public static Object getArrayField(DynamicMessage message,
                                     Descriptors.FieldDescriptor fieldDescriptor,
                                     int index) {
    return message.getRepeatedField(fieldDescriptor, index);
  }
}
