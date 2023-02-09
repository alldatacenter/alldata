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
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.transfer.Transfer.TransferState;
import com.qcloud.cos.utils.CRC64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumableDownloadMonitor implements Callable<File>, TransferMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(ResumableDownloadMonitor.class);

    private final ProgressListenerChain listener;
    private final ResumableDownloadSubmitter downloadSubmitter;
    private final DownloadImpl transfer;
    private final PersistableResumeDownload downloadRecord;
    private final File destFile;
    private final FileChannel destFileChannel;
    private final List<Future<DownloadPart>> futures =
            Collections.synchronizedList(new ArrayList<Future<DownloadPart>>());

    private boolean isDownloadDone = false;
    private AtomicReference<Future<File>> futureReference = new AtomicReference<Future<File>>(null);

    public Future<File> getFuture() {
        return futureReference.get();
    }

    private synchronized void cancelFuture() {
        futureReference.get().cancel(true);
    }

    ResumableDownloadMonitor(ProgressListenerChain listener,
            ResumableDownloadSubmitter downloadSubmitter, DownloadImpl transfer,
            PersistableResumeDownload downloadRecord,
            File destFile, FileChannel destFileChannel) {

        this.listener = listener;
        this.downloadSubmitter = downloadSubmitter;
        this.transfer = transfer;
        this.downloadRecord = downloadRecord;
        this.destFile = destFile;
        this.destFileChannel = destFileChannel;
    }

    public static ResumableDownloadMonitor create(ProgressListenerChain listener,
            ResumableDownloadSubmitter downloadSubmitter, DownloadImpl transfer,
            ExecutorService threadPool, PersistableResumeDownload downloadRecord,
            File destFile, FileChannel destFileChannel) {

        ResumableDownloadMonitor monitor = new ResumableDownloadMonitor(listener, downloadSubmitter,
            transfer, downloadRecord, destFile, destFileChannel);
        monitor.futureReference.compareAndSet(null, threadPool.submit(monitor));
        return monitor;
    }

    public synchronized boolean isDone() {
        return isDownloadDone;
    }

    private synchronized void markAllDone() {
        isDownloadDone = true;
    }

    @Override
    public File call() throws Exception {

        downloadSubmitter.submit();
        futures.addAll(downloadSubmitter.getFutures());

        List<DownloadPart> downloadParts = new ArrayList<DownloadPart>();

        downloadParts.addAll(downloadSubmitter.getSkippedParts());

        try {
            for (Future<DownloadPart> future : futures) {
                try {
                    downloadParts.add(future.get());
                } catch (Exception e) {
                    throw new CosClientException("range download got exception: "+ e.getCause().getMessage() + e.getMessage());
                }
            }

            // download finished.
            downloadRecord.getDumpFile().delete();

            destFileChannel.close();

            if ((downloadRecord.getCrc64ecma() != null) && !downloadRecord.getCrc64ecma().isEmpty()) {
                checkCRC(downloadParts);
            }

            downloadComplete();
            return destFile;
        } catch (CancellationException e) {
            transfer.setState(TransferState.Canceled);
            publishProgress(listener, ProgressEventType.TRANSFER_CANCELED_EVENT);
            throw new CosClientException("Download canceled");
        } catch (Exception e) {
            downloadFailed();
            throw e;
        }
    }

    void downloadComplete() {
        markAllDone();
        transfer.setState(TransferState.Completed);
        publishProgress(listener, ProgressEventType.TRANSFER_COMPLETED_EVENT);
    }
    
    void downloadFailed() {
        transfer.setState(TransferState.Failed);
        publishProgress(listener, ProgressEventType.TRANSFER_FAILED_EVENT);
    }

    private void cancelFutures() {
        cancelFuture();

        if (futures.size() == 0) {
            futures.addAll(downloadSubmitter.getFutures());
        }

        for (Future<DownloadPart> f : futures) {
            f.cancel(true);
        }

        futures.clear();
    }

    void performAbort() {
        cancelFutures();
        publishProgress(listener, ProgressEventType.TRANSFER_CANCELED_EVENT);
    }

    void checkCRC(List<DownloadPart> downloadParts) throws IOException {
        Collections.sort(downloadParts, new Comparator<DownloadPart>(){
            @Override
            public int compare(DownloadPart part1, DownloadPart part2) {
                return (int)(part1.start - part2.start);
            } 
        });

        CRC64 crc64 = new CRC64();

        for (DownloadPart part : downloadParts) {
            crc64 = CRC64.combine(crc64, new CRC64(part.crc64), part.getContentLength());
        }

        long crc64Download = crc64.getValue();
        long crc64Cos = crc64ToLong(downloadRecord.getCrc64ecma());

        log.debug("download crc " + crc64Download + " cos crc " + crc64Cos);

        if (crc64Download != crc64Cos) {
            destFile.delete();
            throw new CosClientException("download file has diff crc64 with cos file, cos: " +
                crc64Cos + " downloaded: " + crc64Download);
        }
    }

    long crc64ToLong(String crc64) {
        if (crc64.charAt(0) == '-') {
            return negativeCrc64ToLong(crc64);
        } else {
            return positiveCrc64ToLong(crc64);
        }
    }

    long positiveCrc64ToLong(String strCrc64) {
        BigInteger crc64 = new BigInteger(strCrc64);
        BigInteger maxLong = new BigInteger(Long.toString(Long.MAX_VALUE));

        int maxCnt = 0;

        while (crc64.compareTo(maxLong) > 0) {
            crc64 = crc64.subtract(maxLong);
            maxCnt++;
        }

        return crc64.longValue() + Long.MAX_VALUE * maxCnt;
    }

    long negativeCrc64ToLong(String strCrc64) {
        BigInteger crc64 = new BigInteger(strCrc64);
        BigInteger minLong = new BigInteger(Long.toString(Long.MIN_VALUE));

        int minCnt = 0;

        while (crc64.compareTo(minLong) < 0) {
            crc64 = crc64.subtract(minLong);
            minCnt++;
        }

        return crc64.longValue() + Long.MIN_VALUE * minCnt;
    }
}
