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

import static com.qcloud.cos.event.SDKProgressPublisher.publishProgress;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.qcloud.cos.COS;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.transfer.Transfer.TransferState;
import com.qcloud.cos.utils.CRC64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ResumableDownloadSubmitter {

    private static final Logger log = LoggerFactory.getLogger(ResumableDownloadSubmitter.class);

    private final COS cos;
    private final ExecutorService threadPool;
    private final GetObjectRequest req;
    private final DownloadImpl download;
    private final File destFile;
    private final RandomAccessFile destRandomAccessFile;
    private final FileChannel destFileChannel;
    private final int partSize;
    private final int multiThreadThreshold;
    private final TransferProgress transferProgress;
    private final ProgressListenerChain listener;

    private PersistableResumeDownload downloadRecord;
    private List<Future<DownloadPart>> futures = new ArrayList<Future<DownloadPart>>();
    private List<DownloadPart> skippedParts = new ArrayList<DownloadPart>();
    
    ResumableDownloadSubmitter(COS cos, ExecutorService threadPool, GetObjectRequest req, DownloadImpl download, File destFile,
            RandomAccessFile destRandomAccessFile, FileChannel destFileChannel, PersistableResumeDownload downloadRecord, int partSize,
            int multiThreadThreshold, TransferProgress transferProgress, ProgressListenerChain listener) {
        if (cos == null || threadPool == null || req == null || download == null || destFile == null || destRandomAccessFile == null || destFileChannel == null || downloadRecord == null)
            throw new IllegalArgumentException("arguments in ResumableDownloadSubmitter must not be null");
        this.cos = cos;
        this.threadPool = threadPool;
        this.req = req;
        this.download = download;
        this.destFile = destFile;
        this.destRandomAccessFile = destRandomAccessFile;
        this.destFileChannel = destFileChannel;
        this.downloadRecord = downloadRecord;
        this.partSize = partSize;
        this.multiThreadThreshold = multiThreadThreshold;
        this.transferProgress = transferProgress;
        this.listener = listener;
    }

    public void submit() throws Exception {
        long contentLength = Long.parseLong(downloadRecord.getContentLength());

        download.setState(TransferState.InProgress);

        if (contentLength < this.multiThreadThreshold) {
            throw new CosClientException("contentLenth " + contentLength + " < " + this.multiThreadThreshold + " should not use resumabledownload.");
        }

        long start = 0;

        publishProgress(listener, ProgressEventType.TRANSFER_STARTED_EVENT);
        while (contentLength > start) {
            long bytesToRead = Math.min(partSize, contentLength - start);
            long end = start + bytesToRead - 1;

            String block = String.format("%d-%d", start, end);
            if (downloadRecord.hasDownloadedBlocks(block)) {
                log.debug("part found in download record: " + block);

                CRC64 crc64 = new CRC64();

                byte[] buffer = new byte[1024*10];
                int readBytes;

                destRandomAccessFile.seek(start);
                while (bytesToRead > 0 && (readBytes = destRandomAccessFile.read(buffer)) != -1) {
                    long updateBytes = Math.min(readBytes, bytesToRead);
                    bytesToRead -= updateBytes;
                    crc64.update(buffer, (int)updateBytes);
                }

                skippedParts.add(new DownloadPart(start, end, crc64.getValue()));

                transferProgress.updateProgress(end + 1 - start);
            } else {
                GetObjectRequest getObj = copyGetObjectRequest(req);
                getObj.setRange(start, end);

                if (threadPool.isShutdown()) {
                    publishProgress(listener, ProgressEventType.TRANSFER_CANCELED_EVENT);
                    throw new CancellationException("TransferManager has been shutdown");
                }
                futures.add(threadPool.submit(new RangeDownloadCallable(cos, getObj, destFile, destFileChannel, downloadRecord)));
            }

            start = end + 1;
        }
    }

    GetObjectRequest copyGetObjectRequest(GetObjectRequest origin) {
        GetObjectRequest dst = new GetObjectRequest(origin.getCOSObjectId());

        dst.setCosCredentials(origin.getCosCredentials());
        dst.setFixedEndpointAddr(origin.getFixedEndpointAddr());
        dst.setGeneralProgressListener(origin.getGeneralProgressListener());
        dst.setMatchingETagConstraints(origin.getMatchingETagConstraints());
        dst.setModifiedSinceConstraint(origin.getModifiedSinceConstraint());
        dst.setNonmatchingETagConstraints(origin.getNonmatchingETagConstraints());
        dst.setResponseHeaders(origin.getResponseHeaders());
        dst.setSSECustomerKey(origin.getSSECustomerKey());
        dst.setTrafficLimit(origin.getTrafficLimit());
        dst.setUnmodifiedSinceConstraint(origin.getUnmodifiedSinceConstraint());

        return dst;
    }

    List<Future<DownloadPart>> getFutures() {
        return futures;
    }

    List<DownloadPart> getSkippedParts() {
        return skippedParts;
    }
}
