/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.pojo.transform;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.pojo.stream.StreamNode;
import org.apache.inlong.manager.common.pojo.transform.TransformDefinition.OperationType;
import org.apache.inlong.manager.common.pojo.transform.TransformDefinition.RuleRelation;
import org.apache.inlong.manager.common.pojo.transform.deduplication.DeDuplicationDefinition;
import org.apache.inlong.manager.common.pojo.transform.deduplication.DeDuplicationDefinition.DeDuplicationStrategy;
import org.apache.inlong.manager.common.pojo.transform.filter.FilterDefinition;
import org.apache.inlong.manager.common.pojo.transform.filter.FilterDefinition.FilterRule;
import org.apache.inlong.manager.common.pojo.transform.filter.FilterDefinition.FilterStrategy;
import org.apache.inlong.manager.common.pojo.transform.filter.FilterDefinition.TargetValue;
import org.apache.inlong.manager.common.pojo.transform.joiner.JoinerDefinition;
import org.apache.inlong.manager.common.pojo.transform.joiner.JoinerDefinition.JoinMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Transform definition service test.
 */
public class TransformDefinitionTest {

    public static Gson gson = new Gson();

    @Test
    public void testParseDeDuplicationDefinition() {
        List<StreamField> streamFields = createStreamFields();
        StreamField timingField = new StreamField(2, FieldType.TIMESTAMP.toString(), "event_time", null, null);
        DeDuplicationDefinition deDuplicationDefinition = new DeDuplicationDefinition(streamFields, timingField, 100,
                TimeUnit.MICROSECONDS, DeDuplicationStrategy.RESERVE_FIRST);
        String definitionJson = gson.toJson(deDuplicationDefinition);
        DeDuplicationDefinition parsedDefinition = gson.fromJson(definitionJson, DeDuplicationDefinition.class);
        Assert.assertEquals(deDuplicationDefinition.getDupFields().size(), parsedDefinition.getDupFields().size());
    }

    @Test
    public void testParseFilterDefinition() {
        List<FilterRule> filterRules = createFilterRule();
        FilterDefinition filterDefinition = new FilterDefinition(FilterStrategy.RETAIN, filterRules);
        String definitionJson = gson.toJson(filterDefinition);
        FilterDefinition parsedDefinition = gson.fromJson(definitionJson, FilterDefinition.class);
        Assert.assertEquals(filterDefinition.getFilterRules().size(), parsedDefinition.getFilterRules().size());
    }

    @Test
    public void testJoinerDefinition() {
        List<StreamField> streamFields = createStreamFields();
        StreamNode leftNode = new BlankStreamNode();
        leftNode.setFieldList(streamFields);
        StreamNode rightNode = new BlankStreamNode();
        rightNode.setFieldList(streamFields);
        JoinerDefinition joinerDefinition = new JoinerDefinition(leftNode, rightNode, streamFields, streamFields,
                JoinMode.INNER_JOIN);
        String definitionJson = gson.toJson(joinerDefinition);
        JoinerDefinition parsedDefinition = gson.fromJson(definitionJson, JoinerDefinition.class);
        Assert.assertEquals(joinerDefinition.getLeftJoinFields().size(), parsedDefinition.getLeftJoinFields().size());
        Assert.assertEquals(joinerDefinition.getRightJoinFields().size(), parsedDefinition.getRightJoinFields().size());
    }

    private List<StreamField> createStreamFields() {
        List<StreamField> streamFieldList = Lists.newArrayList();
        streamFieldList.add(new StreamField(0, FieldType.STRING.toString(), "name", null, null));
        streamFieldList.add(new StreamField(1, FieldType.INT.toString(), "age", null, null));
        return streamFieldList;
    }

    private List<FilterRule> createFilterRule() {
        List<FilterRule> filterRules = Lists.newArrayList();
        filterRules.add(new FilterRule(new StreamField(0, FieldType.STRING.toString(), "name", null, null),
                OperationType.not_null, null, RuleRelation.OR));
        filterRules.add(new FilterRule(new StreamField(1, FieldType.INT.toString(), "age", null, null),
                OperationType.gt, new TargetValue(true, null, "50"), null));
        return filterRules;
    }

    public static class BlankStreamNode extends StreamNode {

    }

}
