/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.test.internal.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.obs.services.internal.utils.RestUtils;

public class RestUtilsTest {
    @Test
    public void test_encodeUrlString() {
        String url = "aa bb.jpg?query=aa bb~*";
        String result = RestUtils.encodeUrlString(url);
        System.out.println(result);
        assertEquals("aa%20bb.jpg%3Fquery%3Daa%20bb~%2A", result);
        
        url = "aa+bb.jpg?query=aa+bb";
        result = RestUtils.encodeUrlString(url);
        System.out.println(result);
        assertEquals("aa%2Bbb.jpg%3Fquery%3Daa%2Bbb", result);
        
    }
    
    @Test
    public void test_uriEncode() {
        String url = "aa bb.jpg?query=aa bb~*";
        String result = RestUtils.uriEncode(url, true);
        System.out.println(result);
        assertEquals("aa bb.jpg?query=aa bb~*", result);
        
        url = "aa+bb.jpg?query=aa+bb";
        result = RestUtils.uriEncode(url, true);
        System.out.println(result);
        assertEquals("aa+bb.jpg?query=aa+bb", result);
        
        url = "aa+bb.jpg?中文=aa+bb";
        result = RestUtils.uriEncode(url, true);
        System.out.println(result);
        assertEquals("aa+bb.jpg?%E4%B8%AD%E6%96%87=aa+bb", result);
        
        url = "aa+bb.jpg?中afda文=aa+b一b";
        result = RestUtils.uriEncode(url, true);
        System.out.println(result);
        assertEquals("aa+bb.jpg?%E4%B8%ADafda%E6%96%87=aa+b%E4%B8%80b", result);
        
        url = "aa+bb.jpg?䲀afda文=aa+b一b";
        result = RestUtils.uriEncode(url, true);
        System.out.println(result);
        assertEquals("aa+bb.jpg?䲀afda%E6%96%87=aa+b%E4%B8%80b", result);
        
    }
}
