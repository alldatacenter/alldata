package com.qcloud.cos.internal;

import org.junit.Test;
import static org.junit.Assert.fail;

public class UrlComponentUtilsTest {

    @Test
    public void testRegionName() {
        UrlComponentUtils.validateRegionName("cos.ap-shanghai");
    }

    @Test
    public void testRegionNameContainWhiteSpace() {
        try {
            UrlComponentUtils.validateRegionName(" ap-shanghai");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("testRegionNameContainWhiteSpace failed");
    }
    
    @Test
    public void testRegionNameContainQuestionMark() {
        try {
            UrlComponentUtils.validateRegionName("ap-shanghai?");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("testRegionNameContainQuestionMark failed");
    }
    
    @Test
    public void testRegionNameContainUpperCaseLetter() {
        try {
            UrlComponentUtils.validateRegionName("Ap-shanghai");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("testRegionNameContainUpperCaseLetter failed");
    }    
    
}
