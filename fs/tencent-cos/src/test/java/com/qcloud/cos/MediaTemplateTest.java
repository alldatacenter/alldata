package com.qcloud.cos;

import com.qcloud.cos.model.ciModel.template.MediaListTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaTemplateObject;
import com.qcloud.cos.model.ciModel.template.MediaTemplateRequest;
import com.qcloud.cos.model.ciModel.template.MediaTemplateResponse;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class MediaTemplateTest extends AbstractCOSClientCITest {

    public static final String TAG = "Animation";
    public static final String FORMAT = "gif";
    public static final String CODEC = "gif";
    public static final String WIDTH = "1280";
    public static final String FPS = "15";
    public static final String ANIMATE_ONLY_KEEP_KEY_FRAME = "true";
    public static final String START = "0";
    public static final String DURATION = "60";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientCITest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientCITest.closeCosClient();
    }

    @Test
    public void mediaTemplateTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        MediaTemplateRequest request = new MediaTemplateRequest();
        request.setBucketName(bucket);
        request.setTag(TAG);
        request.setName(Long.valueOf(System.currentTimeMillis()).toString());
        request.getContainer().setFormat(FORMAT);
        request.getVideo().setCodec(CODEC);
        request.getVideo().setWidth(WIDTH);
        request.getVideo().setFps(FPS);
        request.getVideo().setAnimateOnlyKeepKeyFrame(ANIMATE_ONLY_KEEP_KEY_FRAME);
        request.getTimeInterval().setStart(START);
        request.getTimeInterval().setDuration(DURATION);
        MediaTemplateResponse response = cosclient.createMediaTemplate(request);
        MediaTemplateObject template = response.getTemplate();
        Assert.assertEquals(bucket, template.getBucketId());
        Assert.assertEquals(TAG, template.getTag());
        Assert.assertEquals(FORMAT, template.getTransTpl().getContainer().getFormat());
        Assert.assertEquals(CODEC, template.getTransTpl().getVideo().getCodec());
        Assert.assertEquals(START, template.getTransTpl().getTimeInterval().getStart());

        request = new MediaTemplateRequest();
        request.setBucketName(bucket);
        MediaListTemplateResponse templates = cosclient.describeMediaTemplates(request);
        List<MediaTemplateObject> templateList = templates.getTemplateList();
        Assert.assertTrue(templateList.size() >= 1);
        for (MediaTemplateObject mediaTemplateObject : templateList) {
            if (response.getTemplate().getTemplateId().equals(mediaTemplateObject.getTemplateId())){
                Assert.assertEquals(bucket, mediaTemplateObject.getBucketId());
                Assert.assertEquals(TAG, mediaTemplateObject.getTag());
                Assert.assertEquals(FORMAT, mediaTemplateObject.getTransTpl().getContainer().getFormat());
                Assert.assertEquals(CODEC, mediaTemplateObject.getTransTpl().getVideo().getCodec());
                Assert.assertEquals(START, mediaTemplateObject.getTransTpl().getTimeInterval().getStart());
            }
        }
        request = new MediaTemplateRequest();
        request.setBucketName(bucket);
        request.setTemplateId(response.getTemplate().getTemplateId());
        Boolean aBoolean = cosclient.deleteMediaTemplate(request);
        Assert.assertTrue(aBoolean);
    }

}
