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

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.obs.services.internal.utils.ObjectUtils;

public class ObjectUtilsTest {
    
    @Test
    public void test_isEquals() {
        String str1 = null;
        String str2 = null;
        assertTrue(ObjectUtils.isEquals(str1, str2));
        
        str1 = null;
        str2 = "";
        assertFalse(ObjectUtils.isEquals(str1, str2));
        
        str1 = null;
        str2 = "1";
        assertFalse(ObjectUtils.isEquals(str1, str2));
        
        str1 = "";
        str2 = null;
        assertFalse(ObjectUtils.isEquals(str1, str2));
        
        str1 = "1";
        str2 = null;
        assertFalse(ObjectUtils.isEquals(str1, str2));
        
        str1 = "";
        str2 = "";
        assertTrue(ObjectUtils.isEquals(str1, str2));
        
        str1 = "1";
        str2 = "1";
        assertTrue(ObjectUtils.isEquals(str1, str2));
        
        str1 = "1";
        str2 = "2";
        assertFalse(ObjectUtils.isEquals(str1, str2));
    }
}
