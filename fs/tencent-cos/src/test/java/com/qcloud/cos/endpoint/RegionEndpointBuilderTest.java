package com.qcloud.cos.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

import com.qcloud.cos.region.Region;

public class RegionEndpointBuilderTest {

    @Test
    public void testbuildGeneralApiEndpointCase1() {
        try {
            RegionEndpointBuilder endpointBuilder =
                    new RegionEndpointBuilder(new Region("ap-shanghai"));
            String endpoint = endpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.cos.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("service.cos.myqcloud.com", endpointBuilder.buildGetServiceApiEndpoint());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testbuildGeneralApiEndpointCase2() {
        try {
            RegionEndpointBuilder endpointBuilder =
                    new RegionEndpointBuilder(new Region("cos.ap-shanghai"));
            String endpoint = endpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.cos.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("service.cos.myqcloud.com", endpointBuilder.buildGetServiceApiEndpoint());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testbuildGeneralApiEndpointCase3() {
        try {
            RegionEndpointBuilder endpointBuilder = new RegionEndpointBuilder(null);
            String endpoint = endpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.cos.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("service.cos.myqcloud.com", endpointBuilder.buildGetServiceApiEndpoint());
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }
        fail();
    }
}
