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

package org.apache.ranger.authorization.utils;

import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestStringUtil {
    @Test
    public void testDedupString() {
        Map<String, String> strTbl = new HashMap<>();
        String              s1     = getString("database");
        String              s2     = getString("database");

        // s1 and s2 point to different instances of String
        Assert.assertNotSame("s1 != s2", s1, s2);

        // strTbl doesn't have s1; dedupString(s1) should return s1
        Assert.assertSame("s1 == dedupString(s1)", s1, StringUtil.dedupString(s1, strTbl));

        // strTbl now has s1; s2 has same value as s1, hence dedupString() should return s1
        Assert.assertSame("s1 == dedupString(s2)", s1, StringUtil.dedupString(s2, strTbl));
    }

    @Test
    public void testDedupStringsList() {
        Map<String, String> strTbl = new HashMap<>();
        List<String>        l1     = null;

        Assert.assertSame("null list - dedupStringsList() should return the same list", l1, StringUtil.dedupStringsList(l1, strTbl));

        l1 = Collections.emptyList();
        Assert.assertSame("empty list - dedupStringsList() should return the same list", l1, StringUtil.dedupStringsList(l1, strTbl));

        l1 = new ArrayList<>();
        Assert.assertSame("empty list - dedupStringsList() should return the same list", l1, StringUtil.dedupStringsList(l1, strTbl));

        l1 = new ArrayList<String>() {{ add(getString("*")); }};
        Assert.assertNotSame("non-empty list - dedupStringsList() should return a new list", l1, StringUtil.dedupStringsList(l1, strTbl));

        l1 = new ArrayList<String>() {{ add(getString("*")); add(getString("db1")); }};
        Assert.assertNotSame("non-empty list - dedupStringsList() should return a new list", l1, StringUtil.dedupStringsList(l1, strTbl));

        List<String> l2 = new ArrayList<String>() {{ add(getString("*")); add(getString("db1")); }};

        for (int i = 0; i < l1.size(); i++) {
            Assert.assertNotSame("Before dedupStringsList(): l1[" + i + "] == l2[" + i + "]", l1.get(i), l2.get(i));
        }

        l1 = StringUtil.dedupStringsList(l1, strTbl);
        l2 = StringUtil.dedupStringsList(l2, strTbl);

        for (int i = 0; i < l1.size(); i++) {
            Assert.assertSame("After dedupStringsList(): l1[" + i + "] == l2[" + i + "]", l1.get(i), l2.get(i));
        }
    }

    @Test
    public void testDedupStringsSet() {
        Map<String, String> strTbl = new HashMap<>();
        Set<String>         s1     = null;

        Assert.assertSame("null set - dedupStringsList() should return the same set", s1, StringUtil.dedupStringsSet(s1, strTbl));

        s1 = Collections.emptySet();
        Assert.assertSame("empty set - dedupStringsSet() should return the same set", s1, StringUtil.dedupStringsSet(s1, strTbl));

        s1 = new HashSet<>();
        Assert.assertSame("empty set - dedupStringsSet() should return the same set", s1, StringUtil.dedupStringsSet(s1, strTbl));

        s1 = new HashSet<String>() {{ add(getString("*")); }};
        Assert.assertNotSame("non-empty set - dedupStringsSet() should return a new set", s1, StringUtil.dedupStringsSet(s1, strTbl));

        s1 = new HashSet<String>() {{ add(getString("*")); add(getString("db1")); }};
        Assert.assertNotSame("non-empty set - dedupStringsSet() should return a new set", s1, StringUtil.dedupStringsSet(s1, strTbl));

        Set<String> s2 = new HashSet<String>() {{ add(getString("*")); add(getString("db1")); }};

        for (String elem : s1) {
            Assert.assertFalse("Before dedupStringsSet(): s1[" + elem + "] == s2[" + elem + "]", containsInstance(s2, elem));
        }

        s1 = StringUtil.dedupStringsSet(s1, strTbl);
        s2 = StringUtil.dedupStringsSet(s2, strTbl);

        for (String elem : s1) {
            Assert.assertTrue("After dedupStringsSet(): s1[" + elem + "] == s2[" + elem + "]", containsInstance(s2, elem));
        }
    }

    @Test
    public void testDedupStringsMap() {
        Map<String, String> strTbl = new HashMap<>();
        Map<String, String> m1     = null;

        Assert.assertSame("null map - dedupStringsMap() should return the same map", m1, StringUtil.dedupStringsMap(m1, strTbl));

        m1 = Collections.emptyMap();
        Assert.assertSame("empty map - dedupStringsMap() should return the same map", m1, StringUtil.dedupStringsMap(m1, strTbl));

        m1 = new HashMap<>();
        Assert.assertSame("empty map - dedupStringsMap() should return the same map", m1, StringUtil.dedupStringsMap(m1, strTbl));

        m1 = new HashMap<String, String>() {{ put(getString("database"), getString("*")); }};
        Assert.assertNotSame("non-empty map - dedupStringsMap() should return a new map", m1, StringUtil.dedupStringsMap(m1, strTbl));

        Map<String, String> m2 = new HashMap<String, String>() {{ put(getString("database"), getString("*")); }};

        for (Map.Entry<String, String> entry : m1.entrySet()) {
            String key = entry.getKey();

            Assert.assertFalse("Before dedupStringsMap(): m2 has same key as m1", containsInstance(m2.keySet(), key));
            Assert.assertNotSame("Before dedupStringsMap(): m1[" + key + "] == l2[" + key + "]", m1.get(key), m2.get(key));
        }

        m1 = StringUtil.dedupStringsMap(m1, strTbl);
        m2 = StringUtil.dedupStringsMap(m2, strTbl);

        for (Map.Entry<String, String> entry : m1.entrySet()) {
            String key = entry.getKey();

            Assert.assertTrue("After dedupStringsMap(): m2 has same key as m1", containsInstance(m2.keySet(), key));
            Assert.assertSame("After dedupStringsMap(): m1[" + key + "] == l2[" + key + "]", m1.get(key), m2.get(key));
        }
    }

    @Test
    public void testDedupMapOfPolicyResource() {
        Map<String, String>               strTbl = new HashMap<>();
        Map<String, RangerPolicyResource> m1     = null;

        Assert.assertSame("null map - dedupStringsMapOfPolicyResource() should return the same map", m1, StringUtil.dedupStringsMapOfPolicyResource(m1, strTbl));

        m1 = Collections.emptyMap();
        Assert.assertSame("empty map - dedupStringsMapOfPolicyResource() should return the same map", m1, StringUtil.dedupStringsMapOfPolicyResource(m1, strTbl));

        m1 = new HashMap<>();
        Assert.assertSame("empty map - dedupStringsMapOfPolicyResource() should return the same map", m1, StringUtil.dedupStringsMapOfPolicyResource(m1, strTbl));

        m1 = new HashMap<String, RangerPolicyResource>() {{ put(getString("database"), new RangerPolicyResource(getString("db1"))); put(getString("table"), new RangerPolicyResource(getString("*"))); }};
        Assert.assertNotSame("non-empty map - dedupStringsMapOfPolicyResource() should return a new map", m1, StringUtil.dedupStringsMapOfPolicyResource(m1, strTbl));

        Map<String, RangerPolicyResource> m2 = new HashMap<String, RangerPolicyResource>() {{ put(getString("database"), new RangerPolicyResource(getString("db1"))); put(getString("table"), new RangerPolicyResource(getString("*"))); }};

        for (Map.Entry<String, RangerPolicyResource> entry : m1.entrySet()) {
            String               key    = entry.getKey();
            RangerPolicyResource value1 = entry.getValue();
            RangerPolicyResource value2 = m2.get(key);

            Assert.assertFalse("Before dedupStringsMapOfPolicyResource(): m2 has same key as m1", containsInstance(m2.keySet(), key));

            for (String value : value1.getValues()) {
                Assert.assertFalse("Before dedupStringsMapOfPolicyResource(): m2.values not same values as m1.values for " + value, containsInstance(value2.getValues(), value));
            }
        }

        m1 = StringUtil.dedupStringsMapOfPolicyResource(m1, strTbl);
        m2 = StringUtil.dedupStringsMapOfPolicyResource(m2, strTbl);

        for (Map.Entry<String, RangerPolicyResource> entry : m1.entrySet()) {
            String               key    = entry.getKey();
            RangerPolicyResource value1 = entry.getValue();
            RangerPolicyResource value2 = m2.get(key);

            Assert.assertTrue("After dedupStringsMapOfPolicyResource(): m2 has same key as m1", containsInstance(m2.keySet(), key));

            for (String value : value1.getValues()) {
                Assert.assertTrue("After dedupStringsMapOfPolicyResource(): m2.values has same values as m1.values for " + value, containsInstance(value2.getValues(), value));
            }
        }
    }

    private boolean containsInstance(Collection<String> coll, String key) {
        boolean ret = false;

        if (coll != null) {
            for (String elem : coll) {
                if (elem == key) {
                    ret = true;

                    break;
                }
            }
        }

        return ret;
    }

    private String getString(String str) {
        return str == null ? str : new String(str);
    }
}
