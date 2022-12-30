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

package com.bytedance.bitsail.batch.file.parser;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.ProtoVisitor;
import com.bytedance.bitsail.common.util.ProtobufUtil;
import com.bytedance.bitsail.parser.option.RowParserOptions;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Base64;

/**
 * Created 2020/7/27.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProtobufUtil.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class PbBytesParserTest {

  @Test
  public void testNonBinaryToString() throws Exception {
    PowerMockito.mockStatic(ProtobufUtil.class);
    BitSailConfiguration inputConfiguration = BitSailConfiguration.newDefault();
    inputConfiguration.set(RowParserOptions.PROTO_DESCRIPTOR, "1");
    inputConfiguration.set(RowParserOptions.PROTO_CLASS_NAME, "1");
    inputConfiguration.set(RowParserOptions.PROTO_DESCRIPTOR, Base64.getEncoder().encodeToString("test".getBytes()));
    inputConfiguration.setOption(RowParserOptions.PB_FEATURE.key() + ".binary_string_as_string", "false");

    PbBytesParser parser = new PbBytesParser(inputConfiguration);
    ProtoVisitor.FeatureContext context = parser.getContext();
    Assert.assertFalse(context.isEnabled(ProtoVisitor.Feature.BINARY_STRING_AS_STRING));

    inputConfiguration.setOption(RowParserOptions.PB_FEATURE.key() + ".binary_string_as_string", "true");
    PbBytesParser parserTrue = new PbBytesParser(inputConfiguration);
    ProtoVisitor.FeatureContext contextTrue = parserTrue.getContext();
    Assert.assertTrue(contextTrue.isEnabled(ProtoVisitor.Feature.BINARY_STRING_AS_STRING));

  }

}