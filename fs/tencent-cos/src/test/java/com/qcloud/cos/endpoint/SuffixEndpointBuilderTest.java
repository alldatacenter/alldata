package com.qcloud.cos.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

public class SuffixEndpointBuilderTest {

    @Test
    public void testInitSuffixEndpointBuilderWithNull() {
        try {
            new SuffixEndpointBuilder(null);
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }
        fail();
    }
    
    @Test
    public void testbuildGeneralApiEndpointCase1() {
        try {
            SuffixEndpointBuilder suffixEndpointBuilder = new SuffixEndpointBuilder(".cos.ap-shanghai.myqcloud.com");
            String endpoint = suffixEndpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.cos.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("cos.ap-shanghai.myqcloud.com", suffixEndpointBuilder.buildGetServiceApiEndpoint());
        } catch (Exception e) {
            fail(e.toString());
        }
    }
    
    @Test
    public void testbuildGeneralApiEndpointCase2() {
        try {
            SuffixEndpointBuilder suffixEndpointBuilder = new SuffixEndpointBuilder("cos.ap-shanghai.myqcloud.com");
            String endpoint = suffixEndpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.cos.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("cos.ap-shanghai.myqcloud.com", suffixEndpointBuilder.buildGetServiceApiEndpoint());
        } catch (Exception e) {
            fail(e.toString());
        }
    }
}
