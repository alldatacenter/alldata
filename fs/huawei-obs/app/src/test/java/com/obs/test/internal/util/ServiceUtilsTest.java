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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import com.obs.services.internal.utils.ServiceUtils;

public class ServiceUtilsTest {
    @Test
    public void test_isBucketNameValidDNSName() {
        assertFalse(ServiceUtils.isBucketNameValidDNSName("Test"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("aa"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("."));
        
        assertFalse(ServiceUtils.isBucketNameValidDNSName("192.168.0.1"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("0.0.0.0"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("127.0.0.1"));
        
        assertFalse(ServiceUtils.isBucketNameValidDNSName("test..t"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("test.-t"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("test.t-"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName(".test"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("test-"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("test-"));
        
        
        assertTrue(ServiceUtils.isBucketNameValidDNSName("test.t-t"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("test"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("127.0.0"));
    }
    
    @Test
    public void test_toHex() {
        byte[] hash = new byte[]{-123, 119, 127, 39, 10, -41, -49, 42, 121, 9, -127, -69, -82, 60, 78, 72, 74, 29, -59, 94, 36, -89, 115, -112, -42, -110, -5, -15, -49, -6, 18, -6};
        String result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertEquals("85777f270ad7cf2a790981bbae3c4e484a1dc55e24a77390d692fbf1cffa12fa", result);
        
        hash = new byte[]{};
        result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertEquals("", result);
        
        hash = new byte[]{0};
        result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertEquals("00", result);
        
        hash = new byte[]{9};
        result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertEquals("09", result);
        
        hash = new byte[]{-1};
        result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertEquals("ff", result);
        
        hash = new byte[]{-111};
        result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertEquals("91", result);
        
        hash = null;
        result = ServiceUtils.toHex(hash);
        System.out.println(result);
        assertNull(result);
    }
    
    @Test
    public void test_fromHex() {
        String hexData = "85777f270ad7cf2a790981bbae3c4e484a1dc55e24a77390d692fbf1cffa12fa";
        byte[] result = ServiceUtils.fromHex(hexData);
        System.out.println(Arrays.toString(result));
        assertEquals("[-123, 119, 127, 39, 10, -41, -49, 42, 121, 9, -127, -69, -82, 60, 78, 72, 74, 29, -59, 94, 36, -89, 115, -112, -42, -110, -5, -15, -49, -6, 18, -6]", Arrays.toString(result));
        
        hexData = "85777f";
        result = ServiceUtils.fromHex(hexData);
        System.out.println(Arrays.toString(result));
        assertEquals("[-123, 119, 127]", Arrays.toString(result));
        
        hexData = "";
        result = ServiceUtils.fromHex(hexData);
        System.out.println(Arrays.toString(result));
        assertEquals("[]", Arrays.toString(result));
        
        hexData = null;
        result = ServiceUtils.fromHex(hexData);
        System.out.println(Arrays.toString(result));
        assertEquals("[]", Arrays.toString(result));
        
        hexData = "526ec0573003f19b67d9cdf3a419628a";
        result = ServiceUtils.fromHex(hexData);
        System.out.println(Arrays.toString(result));
        assertEquals("[]", Arrays.toString(result));
    }
    
    @Test
    public void test_fromHex_2() {
        String hexData = "526ec0573003f19b67d9cdf3a419628a";
        byte[] result = ServiceUtils.fromHex(hexData);
        System.out.println(Arrays.toString(result));
        assertEquals("[82, 110, -64, 87, 48, 3, -15, -101, 103, -39, -51, -13, -92, 25, 98, -118]", Arrays.toString(result));
        
        String result2 = ServiceUtils.toBase64(result);
        System.out.println(result2);
        
        assertEquals("Um7AVzAD8Ztn2c3zpBliig==", result2);
    }
    
    @Test
    public void test_isValid() {
        assertFalse(ServiceUtils.isValid(null));
        assertFalse(ServiceUtils.isValid(""));
        assertFalse(ServiceUtils.isValid(" "));
        assertFalse(ServiceUtils.isValid("  "));
        assertTrue(ServiceUtils.isValid(" . "));
    }
    
    @Test
    public void test_isValid2() {
        assertFalse(ServiceUtils.isValid2(null));
        assertFalse(ServiceUtils.isValid2(""));
        assertTrue(ServiceUtils.isValid2(" "));
        assertTrue(ServiceUtils.isValid2("  "));
        assertTrue(ServiceUtils.isValid2(" . "));
    }
    
    @Test
    public void test_toValid() {
        assertEquals("", ServiceUtils.toValid(null));
        assertEquals(" ", ServiceUtils.toValid(" "));
        assertEquals(" a ", ServiceUtils.toValid(" a "));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test_asserParameterNotNull_1() {
        ServiceUtils.asserParameterNotNull(null, "false");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test_asserParameterNotNull_2() {
        ServiceUtils.asserParameterNotNull("", "false");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test_asserParameterNotNull_3() {
        ServiceUtils.asserParameterNotNull(" ", "false");
    }
    
    @Test
    public void test_asserParameterNotNull_4() {
        ServiceUtils.asserParameterNotNull(" . ", "false");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test_asserParameterNotNull2_1() {
        ServiceUtils.asserParameterNotNull2(null, "false");
    }
    
    @Test
    public void test_asserParameterNotNull2_2() {
        ServiceUtils.asserParameterNotNull2(" . ", "false");
    }
}
