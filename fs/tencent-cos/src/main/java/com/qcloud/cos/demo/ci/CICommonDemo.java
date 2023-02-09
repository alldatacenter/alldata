package com.qcloud.cos.demo.ci;

import com.qcloud.cos.internal.Unmarshallers;
import com.qcloud.cos.model.ciModel.job.DocJobResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 数据万象公共接口逻辑demo展示
 */
public class CICommonDemo {

    public static void main(String[] args) throws Exception {
        String src = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Response><EventName>TaskFinish</EventName><JobsDetail><Code>Success</Code><CreationTime>2021-02-19T15:24:44+0800</CreationTime><EndTime>2021-02-19T15:24:45+0800</EndTime><Input><Object>1.docx</Object></Input><JobId>d89c4cf4a728311ebb1229f23e6c9963b</JobId><Message/><Operation><DocProcess><Comments>1</Comments><DocPassword/><EndPage>-1</EndPage><ImageParams/><PaperDirection>0</PaperDirection><PaperSize>0</PaperSize><Quality>100</Quality><SheetId>0</SheetId><SrcType/><StartPage>0</StartPage><TgtType/><Zoom>100</Zoom></DocProcess><DocProcessResult><FailPageCount>0</FailPageCount><PageInfo><PageNo>1</PageNo><TgtUri>test1/1.jpg</TgtUri></PageInfo><SuccPageCount>1</SuccPageCount><TaskId/><TgtType/><TotalPageCount>1</TotalPageCount></DocProcessResult><Output><Bucket>markjrzhang-1251704708</Bucket><Object>test1/${Page}.jpg</Object><Region>ap-chongqing</Region></Output></Operation><QueueId>p3615c32d66fa493bae4c43a5e655ab7f</QueueId><State>Success</State><Tag>DocProcess</Tag></JobsDetail></Response>";
        processCINotifyResponse(src);
    }

    /**
     * 本示例用于解析回调数据，可通过获取Unmarshallers来进行xml数据的解析
     * 可以通过查看CosClient的接口获取到需要使用的xml解析器
     */
    public static void processCINotifyResponse(String response) throws Exception {
        //这里以文档预览的任务回调为例
        Unmarshallers.DescribeDocJobUnmarshaller describeDocJobUnmarshaller = new Unmarshallers.DescribeDocJobUnmarshaller();
        InputStream is = new ByteArrayInputStream(response.getBytes());
        DocJobResponse docJobResponse = describeDocJobUnmarshaller.unmarshall(is);
        System.out.println(docJobResponse);
    }
}
