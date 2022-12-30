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

package com.bytedance.bitsail.dump.datasink.file.parser;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.common.util.ProtoVisitor;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Created 2020/3/31.
 * syntax = "proto3";
 * package com.bytedance.bitsail.dump.datasink.file.parser;
 * option java_outer_classname = "PbParseTest";
 * <p>
 * message ProfileKafkaMsg {
 * int64 app_id = 1;
 * int64 uid = 2;
 * int32 ut = 3;
 * int64 did = 4;
 * int64 timestamp = 5;
 * string profile_namespace = 6;
 * string profile_name = 7;
 * bytes  profile_data = 8;
 * repeated string test_string = 9;
 * SingleMessage single_message = 10;
 * repeated MultiMessage multi_message= 11;
 * map<string,TestValue> inner_properties = 12;
 * }
 * <p>
 * message SingleMessage{
 * map<string, TestValue> projects = 1;
 * }
 * <p>
 * message MultiMessage{
 * map<string, TestValue> proeperties = 10;
 * <p>
 * }
 * <p>
 * message TestValue{
 * repeated int64 id= 1 ;
 * }
 */
public class PbBytesParserTest {

  private static byte[] protobufByteArray;
  private static DynamicMessage dynamicMessage;

  private static byte[] testMapProtobufByteArray;
  private static DynamicMessage testMapDynamicMessage;

  private static byte[] protobufByteArrayCtx;
  private static DynamicMessage dynamicMessageCtx;

  @Before
  public void before() throws InvalidProtocolBufferException {
    PbParseTest.TestValue.Builder newBuilder = PbParseTest.TestValue.newBuilder();
    newBuilder.addId(1L);
    PbParseTest.TestValue testValue = newBuilder.build();

    PbParseTest.SingleMessage.Builder singleMessageBuilder = PbParseTest.SingleMessage.newBuilder();
    singleMessageBuilder.putProjects("single", testValue);
    singleMessageBuilder.putProjects("single1", testValue);

    PbParseTest.MultiMessage.Builder multiMessageBuilder = PbParseTest.MultiMessage.newBuilder();
    multiMessageBuilder.putProeperties("multi", testValue);
    multiMessageBuilder.putProeperties("multi1", testValue);
    multiMessageBuilder.putProeperties("multi2", testValue);

    PbParseTest.ProfileKafkaMsg.Builder builder = PbParseTest.ProfileKafkaMsg.newBuilder();
    PbParseTest.ProfileKafkaMsg build = builder.setAppId(System.currentTimeMillis())
        .setProfileData(ByteString.copyFrom(new byte[] {1, 3, 4, 5}))
        .setSingleMessage(singleMessageBuilder.build())
        .addMultiMessage(multiMessageBuilder.build())
        .putInnerProperties("inner", testValue)
        .putInnerProperties("inner1", testValue)
        .addTestString("SSSSSSSSSS")
        .build();

    protobufByteArray = build.toByteArray();
    dynamicMessage = DynamicMessage.newBuilder(PbParseTest.ProfileKafkaMsg.getDescriptor())
        .mergeFrom(protobufByteArray)
        .build();

    TestMap.collie_log.Builder logBuilder = TestMap.collie_log.newBuilder();
    TestMap.req.Builder reqBuilder = TestMap.req.newBuilder();
    TestMap.req req = reqBuilder.setApiNo(1L)
        .setDeviceId(1L)
        .setJsonStr("Sdsdsdsd")
        .build();
    logBuilder.setReq(req);
    testMapProtobufByteArray = logBuilder.build().toByteArray();
    testMapDynamicMessage = DynamicMessage.newBuilder(TestMap.collie_log.getDescriptor())
        .mergeFrom(testMapProtobufByteArray)
        .build();

    PbParseTest.TestValue.Builder newBuilderCtx = PbParseTest.TestValue.newBuilder();
    newBuilderCtx.addId(1L);
    PbParseTest.TestValue testValueCtx = newBuilder.build();

    PbParseTest.SingleMessage.Builder singleMessageBuilderCtx = PbParseTest.SingleMessage.newBuilder();
    singleMessageBuilderCtx.putProjects("single", testValue);
    singleMessageBuilderCtx.putProjects("single1", testValue);

    PbParseTest.MultiMessage.Builder multiMessageBuilderCtx = PbParseTest.MultiMessage.newBuilder();
    multiMessageBuilderCtx.putProeperties("multi", testValue);
    multiMessageBuilderCtx.putProeperties("multi1", testValue);
    multiMessageBuilderCtx.putProeperties("multi2", testValue);

    Map<String, Long> primaryMapCtx = Maps.newHashMap();
    primaryMapCtx.put("test", 1L);

    PbParseTest.ProfileKafkaMsg.Builder builderCtx = PbParseTest.ProfileKafkaMsg.newBuilder();
    PbParseTest.ProfileKafkaMsg buildCtx = builderCtx
        .setProfileData(ByteString.copyFrom(new byte[] {1, 3, 4, 5}))
        .setSingleMessage(singleMessageBuilderCtx.build())
        .addMultiMessage(multiMessageBuilderCtx.build())
        .putInnerProperties("inner", testValueCtx)
        .putInnerProperties("inner1", testValueCtx)
        .putAllPrimiaryMap(primaryMapCtx)
        .build();

    protobufByteArrayCtx = buildCtx.toByteArray();
    dynamicMessageCtx = DynamicMessage.newBuilder(PbParseTest.ProfileKafkaMsg.getDescriptor())
        .mergeFrom(protobufByteArrayCtx)
        .build();
  }

  @Test
  public void testGetNestJsonValue() {
    ColumnInfo columnInfo2 = ColumnInfo.builder()
        .name("inner_properties")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo2 = FieldPathUtils.getPathInfo(columnInfo2.getName());
    Object pathValue2 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo2);
    Assert.assertNotNull(pathValue2);
    Assert.assertTrue(pathValue2 instanceof Map);

    ColumnInfo columnInfo1 = ColumnInfo.builder()
        .name("multi_message")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo1 = FieldPathUtils.getPathInfo(columnInfo1.getName());
    Object pathValue1 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo1);
    Assert.assertNotNull(pathValue1);
    Assert.assertTrue(pathValue1 instanceof List);

    ColumnInfo columnInfo3 = ColumnInfo.builder()
        .name("inner_properties.inner.id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo3 = FieldPathUtils.getPathInfo(columnInfo3.getName());
    Object pathValue3 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo3);
    Assert.assertNotNull(pathValue3);
    Assert.assertTrue(pathValue3 instanceof List);

    ColumnInfo columnInfo4 = ColumnInfo.builder()
        .name("multi_message[0].proeperties.multi.id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo4 = FieldPathUtils.getPathInfo(columnInfo4.getName());
    Object pathValue4 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo4);
    Assert.assertNotNull(pathValue4);
    Assert.assertTrue(pathValue4 instanceof List);

    ColumnInfo columnInfo5 = ColumnInfo.builder()
        .name("test_string")
        .type("string").build();

    FieldPathUtils.PathInfo pathInfo5 = FieldPathUtils.getPathInfo(columnInfo5.getName());
    Object pathValue5 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo5);
    Assert.assertNotNull(pathValue5);
    Assert.assertTrue(pathValue5 instanceof List);

    ColumnInfo columnInfo6 = ColumnInfo.builder()
        .name("single_message.projects.single.id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo6 = FieldPathUtils.getPathInfo(columnInfo6.getName());
    Object pathValue6 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo6);
    Assert.assertNotNull(pathValue6);
    Assert.assertTrue(pathValue6 instanceof List);

    ColumnInfo columnInfo7 = ColumnInfo.builder()
        .name("app_id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo7 = FieldPathUtils.getPathInfo(columnInfo7.getName());
    Object pathValue7 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo7);
    Assert.assertNotNull(pathValue7);
    Assert.assertTrue(pathValue7 instanceof Long);

    ColumnInfo columnInfo8 = ColumnInfo.builder()
        .name("test_string[0]")
        .type("string").build();

    FieldPathUtils.PathInfo pathInfo8 = FieldPathUtils.getPathInfo(columnInfo8.getName());
    Object pathValue8 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo8);
    Assert.assertNotNull(pathValue8);
    Assert.assertTrue(pathValue8 instanceof String);

    ColumnInfo columnInfo9 = ColumnInfo.builder()
        .name("single_message")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo9 = FieldPathUtils.getPathInfo(columnInfo9.getName());
    Object pathValue9 = ProtoVisitor.getPathValue(dynamicMessage, pathInfo9);
    Assert.assertNotNull(pathValue9);
    Assert.assertTrue(pathValue9 instanceof Map);
  }

  @Test
  public void testReqProtobuf() {
    ColumnInfo columnInfo1 = ColumnInfo.builder()
        .name("req")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo1 = FieldPathUtils.getPathInfo(columnInfo1.getName());
    Object pathValue1 = ProtoVisitor.getPathValue(testMapDynamicMessage, pathInfo1);
    Assert.assertNotNull(pathValue1);
    Assert.assertTrue(pathValue1 instanceof Map);
    Assert.assertEquals(3, CollectionUtils.size(pathValue1));
  }

  @Test
  public void testGetNestJsonValueWithCtx() {
    ProtoVisitor.FeatureContext featureContext = ProtoVisitor.FeatureContext.defaultContext();
    featureContext.disableFeature(ProtoVisitor.Feature.USE_DEFAULT_VALUE);

    ColumnInfo columnInfo2 = ColumnInfo.builder()
        .name("inner_properties")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo2 = FieldPathUtils.getPathInfo(columnInfo2.getName());
    Object pathValue2 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo2, featureContext);
    Assert.assertNotNull(pathValue2);
    Assert.assertTrue(pathValue2 instanceof Map);

    ColumnInfo columnInfo1 = ColumnInfo.builder()
        .name("multi_message")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo1 = FieldPathUtils.getPathInfo(columnInfo1.getName());
    Object pathValue1 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo1, featureContext);
    Assert.assertNotNull(pathValue1);
    Assert.assertTrue(pathValue1 instanceof List);

    ColumnInfo columnInfo3 = ColumnInfo.builder()
        .name("inner_properties.inner.job_id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo3 = FieldPathUtils.getPathInfo(columnInfo3.getName());
    Object pathValue3 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo3, featureContext);
    Assert.assertNull(pathValue3);
    Object pathValue3default = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo3, ProtoVisitor.FeatureContext.defaultContext());
    Assert.assertEquals(pathValue3default, 0L);

    ColumnInfo columnInfo4 = ColumnInfo.builder()
        .name("multi_message[0].proeperties.multi.id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo4 = FieldPathUtils.getPathInfo(columnInfo4.getName());
    Object pathValue4 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo4, featureContext);
    Assert.assertNotNull(pathValue4);
    Assert.assertTrue(pathValue4 instanceof List);

    ColumnInfo columnInfo5 = ColumnInfo.builder()
        .name("test_string")
        .type("string").build();

    FieldPathUtils.PathInfo pathInfo5 = FieldPathUtils.getPathInfo(columnInfo5.getName());
    Object pathValue5 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo5, featureContext);
    Assert.assertNotNull(pathValue5);
    Assert.assertTrue(pathValue5 instanceof List);

    ColumnInfo columnInfo6 = ColumnInfo.builder()
        .name("single_message.projects.single.id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo6 = FieldPathUtils.getPathInfo(columnInfo6.getName());
    Object pathValue6 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo6, featureContext);
    Assert.assertNotNull(pathValue6);
    Assert.assertTrue(pathValue6 instanceof List);

    ColumnInfo columnInfo7 = ColumnInfo.builder()
        .name("app_id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo7 = FieldPathUtils.getPathInfo(columnInfo7.getName());
    Object pathValue7 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo7, featureContext);
    Assert.assertNull(pathValue7);
    Object pathValue71 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo7, ProtoVisitor.FeatureContext.defaultContext());
    Assert.assertEquals(pathValue71, 0L);

    ColumnInfo columnInfo8 = ColumnInfo.builder()
        .name("test_string[0]")
        .type("string").build();

    FieldPathUtils.PathInfo pathInfo8 = FieldPathUtils.getPathInfo(columnInfo8.getName());
    Object pathValue8 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo8, featureContext);
    Assert.assertNull(pathValue8);

    ColumnInfo columnInfo9 = ColumnInfo.builder()
        .name("single_message.single_app_id")
        .type("bigint").build();

    FieldPathUtils.PathInfo pathInfo9 = FieldPathUtils.getPathInfo(columnInfo9.getName());
    Object pathValue9 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo9, featureContext);
    Assert.assertNull(pathValue9);
    Object pathValue91 = ProtoVisitor.getPathValue(dynamicMessageCtx, pathInfo9, ProtoVisitor.FeatureContext.defaultContext());
    Assert.assertEquals(pathValue91, 0L);
  }
}