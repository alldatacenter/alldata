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

package com.bytedance.bitsail.connector.elasticsearch.doc;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.doc.parameter.EsDocParameters;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;
import com.bytedance.bitsail.connector.elasticsearch.rest.EsRequestEmitter;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class EsDocConstructorTest {

  private final Object[] fields = new Object[] {
      100,
      "varchar",
      "text",
      "bigint",
      "20220810",
      "es_index_20220810",
      "index",
      10
  };
  private final Row row = new Row(fields);

  @Test
  public void testConstructDoc() throws Exception {
    BitSailConfiguration jobConf = JobConfUtils.fromClasspath("es_doc_parameter_test.json");
    jobConf.set(ElasticsearchWriterOptions.DOC_EXCLUDE_FIELDS,
        "op_type,version");

    EsDocParameters parameters = EsRequestEmitter.initEsDocParams(jobConf);
    EsDocConstructor constructor = new EsDocConstructor(parameters);
    String doc = constructor.form(row);

    JSONObject jsonObject = JSON.parseObject(doc);
    Assert.assertEquals("20220810", jsonObject.getString("date"));
    Assert.assertEquals("text", jsonObject.getString("text_type"));
    Assert.assertEquals("varchar", jsonObject.getString("varchar_type"));
    Assert.assertEquals("bigint", jsonObject.getString("bigint_type"));
    Assert.assertEquals(100, jsonObject.getInteger("id").intValue());
  }
}
