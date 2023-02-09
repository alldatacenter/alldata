package com.qcloud.cos;

import com.qcloud.cos.exception.CosServiceException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class AppendObjectTest extends AbstractCOSClientTest{
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }
    // 测试上传文件，append大小为4K
    @Test
    public void testAppendGetDelObject4K() throws CosServiceException, IOException {
        testAppendGetDelObjectDiffSize(4096, false);
        testAppendGetDelObjectDiffSize(4096, true);
    }
    // 测试上传文件,append的大小为1M
    @Test
    public void testAppendGetDelObject1M() throws CosServiceException, IOException {
        testAppendGetDelObjectDiffSize(1024*1024, false);
        testAppendGetDelObjectDiffSize(1024*1024, true);
    }
}
