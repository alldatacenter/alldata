package com.qcloud.cos.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;

public class DefaultEndpointResolverTest {
    @Test
    public void test() {
        DefaultEndpointResolver resolver = new DefaultEndpointResolver();
        String generapiEndpoint = "xxx-123.cos.ap-beijing-1.myqcloud.com";
        String getServiceApiEndpoint = "service.cos.myqcloud.com";
        assertEquals(generapiEndpoint, resolver.resolveGeneralApiEndpoint(generapiEndpoint));
        assertEquals(getServiceApiEndpoint, resolver.resolveGetServiceApiEndpoint(getServiceApiEndpoint));
    }

}
