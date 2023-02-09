package com.obs.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.obs.services.internal.V2Convertor;
import com.obs.services.model.RequestPaymentEnum;

public class V2ConvertorTest {
    @Test
    public void test_bucket_owner_xml() {
        RequestPaymentEnum payer = RequestPaymentEnum.BUCKET_OWNER;
        
        String content = V2Convertor.getInstance().transRequestPaymentConfiguration("test", payer.getCode());
        assertEquals(getRequestPaymentXML(payer), content);
    }
    
    @Test
    public void test_requester_xml() {
        RequestPaymentEnum payer = RequestPaymentEnum.REQUESTER;
        
        String content = V2Convertor.getInstance().transRequestPaymentConfiguration("test", payer.getCode());
        assertEquals(getRequestPaymentXML(payer), content);
    }
    
    public String getRequestPaymentXML(RequestPaymentEnum payer) {
        return "<RequestPaymentConfiguration><Payer>" + payer.getCode() + "</Payer></RequestPaymentConfiguration>";
    }
}
