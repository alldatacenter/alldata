/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.component;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.citrus.turbine.impl.DefaultContext;
import com.alibaba.fastjson.JSONArray;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ValidatorCommons;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DefaultFieldErrorHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DelegateControl4JsonPostMsgHandler;
import com.qlangtech.tis.util.AttrValMap;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * @create: 2020-02-05 15:55
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestRockMqPluginValidate extends BaseTestCase {

    final String mqTopic = "mqTopic";

    final String deserialize = "deserialize";

    final String consumeName = "consumeName";

    final String namesrvAddr = "namesrvAddr";

    final String testProp = "testProp";

    public void testDigital_Alpha_CharacterValidate() throws Exception {
        final DefaultContext context = new DefaultContext();
        final Map<String, Object> fieldErrors = context.getContextMap();
        validatePluginPostForm("rockmq_plugin_from_invalid.json", context);

        List<List<DefaultFieldErrorHandler.FieldError>> /** item*/
                itemsErrorList = (List<List<DefaultFieldErrorHandler.FieldError>>) fieldErrors.get(IFieldErrorHandler.ACTION_ERROR_FIELDS);

        assertNotNull(itemsErrorList);
        assertEquals(1, itemsErrorList.size());
        List<DefaultFieldErrorHandler.FieldError> fieldErrors1 = itemsErrorList.get(0);
        assertEquals(2, fieldErrors1.size());
        Optional<DefaultFieldErrorHandler.FieldError> mqTopicErr = fieldErrors1.stream().filter((r) -> mqTopic.equals(r.getFieldName())).findFirst();
        assertTrue(mqTopicErr.isPresent());
        assertEquals(ValidatorCommons.MSG_IDENTITY_ERROR, mqTopicErr.get().getMsg());
        assertNull(mqTopicErr.get().itemsErrorList);
        assertEquals(mqTopic, mqTopicErr.get().getFieldName());
        Optional<DefaultFieldErrorHandler.FieldError> deserializeErr = fieldErrors1.stream().filter((r) -> deserialize.equals(r.getFieldName())).findFirst();
        assertTrue(deserializeErr.isPresent());
        DefaultFieldErrorHandler.FieldError dErr = deserializeErr.get();
        assertEquals(deserialize, dErr.getFieldName());
        assertNull(dErr.getMsg());
        assertNotNull(dErr.itemsErrorList);
        assertEquals(1, dErr.itemsErrorList.size());
        List<DefaultFieldErrorHandler.FieldError> /**
         * item
         */
                dValsItem = dErr.itemsErrorList.get(0);
        assertEquals(1, dValsItem.size());
        DefaultFieldErrorHandler.FieldError testName = dValsItem.get(0);
        assertEquals(testProp, testName.getFieldName());
        assertEquals("ddd", testName.getMsg());
        assertNull(testName.itemsErrorList);
    }

    public void testEmptyInputValidate() throws Exception {
        // final Map<String, String> fieldErrors = Maps.newHashMap();
        final DefaultContext context = new DefaultContext();
        final Map<String, Object> fieldErrors = context.getContextMap();
        validatePluginPostForm("rockmq_plugin_from_empty.json", context);
        List<List<DefaultFieldErrorHandler.FieldError>> /**
         * item
         */
                itemsErrorList = (List<List<DefaultFieldErrorHandler.FieldError>>) fieldErrors.get(IFieldErrorHandler.ACTION_ERROR_FIELDS);
        assertEquals(1, itemsErrorList.size());
        List<DefaultFieldErrorHandler.FieldError> /**
         * item
         */
                fErrors = itemsErrorList.get(0);
        assertEquals(4, fErrors.size());
        Map<String, DefaultFieldErrorHandler.FieldError> filedErrorMap = fErrors.stream().collect(Collectors.toMap((r) -> r.getFieldName(), (r) -> r));
        assertNotNull(filedErrorMap.get(mqTopic));
        assertNotNull(filedErrorMap.get(deserialize));
        assertNotNull(filedErrorMap.get(consumeName));
        assertNotNull(filedErrorMap.get(namesrvAddr));
        for (DefaultFieldErrorHandler.FieldError errMsg : filedErrorMap.values()) {
            assertEquals("ddd", errMsg.getMsg());
        }
    }

    private void validatePluginPostForm(String jsonPath, Context context) throws IOException {
        //  DefaultFieldErrorHandler fieldErrorHandler = new DefaultFieldErrorHandler();

        DelegateControl4JsonPostMsgHandler fieldErrorHandler = null;

        List<AttrValMap> attrValMaps = null;
        try {
            try (InputStream reader = this.getClass().getResourceAsStream(jsonPath)) {
                JSONArray itemsArray = JSONArray.parseArray(IOUtils.toString(reader, TisUTF8.get()));
                attrValMaps = AttrValMap.describableAttrValMapList(itemsArray, Optional.empty());
            }
        } catch (Exception e) {
            throw new IllegalStateException("jsonPath:" + jsonPath, e);
        }
        assertNotNull(attrValMaps);
        assertEquals(1, attrValMaps.size());
        AttrValMap attrValMap = attrValMaps.get(0);
        Descriptor.PluginValidateResult validateResult = attrValMap.validate(fieldErrorHandler, context, true);
        assertFalse("validate false", validateResult.isValid());
    }
}
