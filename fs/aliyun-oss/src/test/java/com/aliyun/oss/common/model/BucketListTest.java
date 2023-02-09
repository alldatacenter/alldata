/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.ListBucketsRequest;

public class BucketListTest {

   @Test
   public void testBucketList() {
       BucketList bucketList = new BucketList();
       bucketList.setPrefix("prefix");
       bucketList.setMarker("marker");
       bucketList.setMaxKeys(Integer.valueOf(6));
       bucketList.setTruncated(true);
       bucketList.setNextMarker("nextMarker");
       List<Bucket> buckets = new ArrayList<Bucket>();
       Bucket bucket = new Bucket();
       bucket.setName("name");
       bucket.setLocation("osslocation");
       buckets.add(bucket);
       bucketList.clearBucketList();
       bucketList.setBucketList(buckets);
       assertEquals("prefix", bucketList.getPrefix());
       assertEquals("marker", bucketList.getMarker());
       assertEquals(6, bucketList.getMaxKeys().intValue());
       assertEquals("nextMarker", bucketList.getNextMarker());
       assertEquals(true, bucketList.isTruncated());
       assertEquals(1, bucketList.getBucketList().size());
       buckets = bucketList.getBucketList();
       bucket = buckets.get(0);
       assertEquals("name", bucket.getName());
       assertEquals("osslocation", bucket.getLocation());

       bucketList.setBucketList(null);
       assertEquals(0, bucketList.getBucketList().size());

       buckets.clear();
       bucketList.setBucketList(buckets);
       assertEquals(0, bucketList.getBucketList().size());
   }

   @Test
   public void testListBucketRequest() {
       ListBucketsRequest request = new ListBucketsRequest("prefix", "marker", Integer.valueOf(1000));
       assertEquals("prefix", request.getPrefix());
       assertEquals("marker", request.getMarker());
       assertEquals(Integer.valueOf(1000), request.getMaxKeys());

       request = request.withPrefix("prefix0").withMarker("marker20").withMaxKeys(20);

       request.setPrefix("prefix2");
       request.setMarker("marker2");
       request.setMaxKeys(Integer.valueOf(1));
       assertEquals("prefix2", request.getPrefix());
       assertEquals("marker2", request.getMarker());
       assertEquals(Integer.valueOf(1), request.getMaxKeys());
       try {
           request.setMaxKeys(Integer.valueOf(1001));
           assertTrue(false);
       } catch (IllegalArgumentException e) {
       }
       try {
           request.setMaxKeys(Integer.valueOf(-1));
           assertTrue(false);
       } catch (IllegalArgumentException e) {
       }
   }
}
