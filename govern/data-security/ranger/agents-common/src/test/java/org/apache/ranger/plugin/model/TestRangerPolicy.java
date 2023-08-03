/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.model;


import org.junit.Assert;
import org.junit.Test;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;

import java.util.ArrayList;
import java.util.List;

public class TestRangerPolicy {

    @Test
    public void test_01_Policy_SetListMethods() {
        RangerPolicy           policy         = new RangerPolicy();
        List<RangerPolicyItem> policyItemList = getList(new RangerPolicyItem());

        Assert.assertEquals("RangerPolicy.getPolicyItems()", 0, policy.getPolicyItems().size());
        policy.getPolicyItems().add(new RangerPolicyItem());
        Assert.assertEquals("RangerPolicy.getPolicyItems().add()", 1, policy.getPolicyItems().size());
        policy.setPolicyItems(policyItemList);
        Assert.assertEquals("RangerPolicy.setPolicyItems()", policyItemList.size(), policy.getPolicyItems().size());

        Assert.assertEquals("RangerPolicy.getDenyPolicyItems()", 0, policy.getDenyPolicyItems().size());
        policy.getDenyPolicyItems().add(new RangerPolicyItem());
        Assert.assertEquals("RangerPolicy.getDenyPolicyItems().add()", 1, policy.getDenyPolicyItems().size());
        policy.setDenyPolicyItems(policyItemList);
        Assert.assertEquals("RangerPolicy.setDenyPolicyItems()", policyItemList.size(), policy.getDenyPolicyItems().size());

        Assert.assertEquals("RangerPolicy.getAllowExceptions()", 0, policy.getAllowExceptions().size());
        policy.getAllowExceptions().add(new RangerPolicyItem());
        Assert.assertEquals("RangerPolicy.getAllowExceptions().add()", 1, policy.getAllowExceptions().size());
        policy.setAllowExceptions(policyItemList);
        Assert.assertEquals("RangerPolicy.setAllowExceptions()", policyItemList.size(), policy.getAllowExceptions().size());

        Assert.assertEquals("RangerPolicy.getDenyExceptions()", 0, policy.getDenyExceptions().size());
        policy.getDenyExceptions().add(new RangerPolicyItem());
        Assert.assertEquals("RangerPolicy.getDenyExceptions().add()", 1, policy.getDenyExceptions().size());
        policy.setDenyExceptions(policyItemList);
        Assert.assertEquals("RangerPolicy.setDenyExceptions()", policyItemList.size(), policy.getDenyExceptions().size());
    }

    @Test
    public void test_02_PolicyItem_SetListMethods() {
        RangerPolicyItem                policyItem = new RangerPolicyItem();
        List<RangerPolicyItemAccess>    accesses   = getList(new RangerPolicyItemAccess());
        List<String>                    users      = getList("user");
        List<String>                    groups     = getList("group");
        List<RangerPolicyItemCondition> conditions = getList(new RangerPolicyItemCondition());


        Assert.assertEquals("RangerPolicyItem.getAccesses()", 0, policyItem.getAccesses().size());
        policyItem.getAccesses().add(new RangerPolicyItemAccess());
        Assert.assertEquals("RangerPolicyItem.getAccesses().add()", 1, policyItem.getAccesses().size());
        policyItem.setAccesses(accesses);
        Assert.assertEquals("RangerPolicyItem.setAccesses()", accesses.size(), policyItem.getAccesses().size());

        Assert.assertEquals("RangerPolicyItem.getUsers()", 0, policyItem.getUsers().size());
        policyItem.getUsers().add(new String());
        Assert.assertEquals("RangerPolicyItem.getUsers().add()", 1, policyItem.getUsers().size());
        policyItem.setUsers(users);
        Assert.assertEquals("RangerPolicyItem.setUsers()", users.size(), policyItem.getUsers().size());

        Assert.assertEquals("RangerPolicyItem.getGroups()", 0, policyItem.getGroups().size());
        policyItem.getGroups().add(new String());
        Assert.assertEquals("RangerPolicyItem.getGroups().add()", 1, policyItem.getGroups().size());
        policyItem.setGroups(groups);
        Assert.assertEquals("RangerPolicyItem.setGroups()", groups.size(), policyItem.getGroups().size());

        Assert.assertEquals("RangerPolicyItem.getConditions()", 0, policyItem.getConditions().size());
        policyItem.getConditions().add(new RangerPolicyItemCondition());
        Assert.assertEquals("RangerPolicyItem.getConditions().add()", 1, policyItem.getConditions().size());
        policyItem.setConditions(conditions);
        Assert.assertEquals("RangerPolicyItem.setConditions()", conditions.size(), policyItem.getConditions().size());
    }

    @Test
    public void test_03_PolicyResource_SetListMethods() {
        RangerPolicyResource policyResource = new RangerPolicyResource();
        List<String>         values         = getList("value");

        Assert.assertEquals("RangerPolicyResource.getValues()", 0, policyResource.getValues().size());
        policyResource.getValues().add(new String());
        Assert.assertEquals("RangerPolicyResource.getValues().add()", 1, policyResource.getValues().size());
        policyResource.setValues(values);
        Assert.assertEquals("RangerPolicyResource.setValues()", values.size(), policyResource.getValues().size());
    }

    @Test
    public void test_04_PolicyItemCondition_SetListMethods() {
        RangerPolicyItemCondition policyItemCondition = new RangerPolicyItemCondition();
        List<String>              values              = getList("value");

        Assert.assertEquals("RangerPolicyItemCondition.getValues()", 0, policyItemCondition.getValues().size());
        policyItemCondition.getValues().add(new String());
        Assert.assertEquals("RangerPolicyItemCondition.getValues().add()", 1, policyItemCondition.getValues().size());
        policyItemCondition.setValues(values);
        Assert.assertEquals("RangerPolicyItemCondition.setValues()", values.size(), policyItemCondition.getValues().size());
    }

    private <T> List<T> getList(T value) {
        List<T> ret = new ArrayList<>();

        int count = getRandomNumber(10);
        for(int i = 0; i < count; i ++) {
            ret.add(value);
        }

        return ret;
    }

    private int getRandomNumber(int maxValue) {
        return (int)(Math.random() * maxValue);
    }
}
