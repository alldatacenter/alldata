/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.transfer;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;

import com.qcloud.cos.COS;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.utils.CRC64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangeDownloadCallable implements Callable<DownloadPart> {

    private static final Logger log = LoggerFactory.getLogger(RangeDownloadCallable.class);

    private final COS cos;
    private final GetObjectRequest request; 
    private final File destFile;
    private FileChannel destFileChannel;
    private PersistableResumeDownload downloadRecord;
    
    public RangeDownloadCallable(COS cos, GetObjectRequest request,
            File destFile, FileChannel destFileChannel,
            PersistableResumeDownload downloadRecord) {
        this.cos = cos;
        this.request = request;
        this.destFile = destFile;
        this.destFileChannel = destFileChannel;
        this.downloadRecord = downloadRecord;
    }

    public DownloadPart call() throws Exception {
        COSObject object = cos.getObject(request);
        InputStream input = object.getObjectContent(); 
        ObjectMetadata meta = object.getObjectMetadata();

        long[] range = request.getRange();
        long start = range[0];
        long end = range[1];

        long position = start;

        ByteBuffer tmpBuf = ByteBuffer.allocateDirect(1024*1024);

        byte[] buffer = new byte[1024*10];
        int bytesRead;

        CRC64 crc64 = new CRC64();

        while ((bytesRead = input.read(buffer)) > -1) {
            start += bytesRead;

            crc64.update(buffer, bytesRead);

            if (tmpBuf.remaining() < bytesRead) {
                tmpBuf.flip();
                position += destFileChannel.write(tmpBuf, position);
                tmpBuf.clear();
            }

            tmpBuf.put(buffer, 0, bytesRead);
        }

        tmpBuf.flip();
        destFileChannel.write(tmpBuf, position);

        if (start != end + 1) {
            destFileChannel.close();
            destFile.delete();
            String msg = String.format("get object want %d bytes, but got %d bytes, reqeust_id: %s",
                end + 1, start, meta.getRequestId());
            throw new CosClientException(msg);
        }

        String block = String.format("%d-%d", range[0], range[1]);
        downloadRecord.putDownloadedBlocks(block);
        downloadRecord.dump();

        return new DownloadPart(range[0], range[1], crc64.getValue());
    }
}
