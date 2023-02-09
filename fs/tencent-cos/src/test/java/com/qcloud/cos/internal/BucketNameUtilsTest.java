package com.qcloud.cos.internal;

import static org.junit.Assert.*;

import org.junit.Test;

public class BucketNameUtilsTest {

    @Test
    public void testBucketNameNormal() {
        BucketNameUtils.validateBucketName("a1234");
    }
    
    @Test
    public void testBucketNameLongerThan40() {
        try {
            BucketNameUtils.validateBucketName("abc12abc12abc12abc12abc12abc12abc12abc121");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test long bucket name fail");
    }
    
    @Test
    public void testBucketNameEmpty() {
        try {
            BucketNameUtils.validateBucketName("");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test empty bucket name fail");
    }
    
    @Test
    public void testBucketNameContailUpperCase() {
        try {
            BucketNameUtils.validateBucketName("Aa");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test bucket name contain upper case fail");
    }
    
    @Test
    public void testBucketNameContainSpace() {
        try {
            BucketNameUtils.validateBucketName("a b");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test bucket name contain space fail");
    }
    
    @Test
    public void testBucketNameContainTab() {
        try {
            BucketNameUtils.validateBucketName("a\tb");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test bucket name contain tab fail");
    }
    
    @Test
    public void testBucketNameContainCarriageReturn() {
        try {
            BucketNameUtils.validateBucketName("a\rb");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test bucket name contain carriage fail");
    }

    @Test
    public void testBucketNameContainLineBreak() {
        try {
            BucketNameUtils.validateBucketName("a\nb");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test bucket name contain line break fail");
    }
    
    @Test
    public void testBucketNameContainOtherInvalidLetter() {
        try {
            BucketNameUtils.validateBucketName("a+b");
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("test bucket name contain invalid letter fail");
    }
    
}
