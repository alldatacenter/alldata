package com.qcloud.cos.endpoint;

import com.qcloud.cos.region.Region;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CIRegionEndpointBuilderTest {

    @Test
    public void testbuildGeneralApiEndpointCase1() {
        try {
            CIRegionEndpointBuilder endpointBuilder =
                    new CIRegionEndpointBuilder(new Region("ap-shanghai"));
            String endpoint = endpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.ci.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("service.ci.myqcloud.com", endpointBuilder.buildGetServiceApiEndpoint());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testbuildGeneralApiEndpointCase2() {
        try {
            CIRegionEndpointBuilder endpointBuilder =
                    new CIRegionEndpointBuilder(new Region("shanghai"));
            String endpoint = endpointBuilder.buildGeneralApiEndpoint("xxx-1251000");
            assertEquals("xxx-1251000.ci.ap-shanghai.myqcloud.com", endpoint);
            assertEquals("service.ci.myqcloud.com", endpointBuilder.buildGetServiceApiEndpoint());
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void testbuildGeneralApiEndpointCase3() {
        try {
            CIRegionEndpointBuilder endpointBuilder = new CIRegionEndpointBuilder(null);
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
