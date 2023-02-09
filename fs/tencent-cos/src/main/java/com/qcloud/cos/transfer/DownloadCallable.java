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
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLProtocolException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos.COS;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.FileLockException;
import com.qcloud.cos.internal.FileLocks;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.transfer.Transfer.TransferState;
import com.qcloud.cos.utils.ServiceUtils;
import com.qcloud.cos.utils.ServiceUtils.RetryableCOSDownloadTask;

final class DownloadCallable implements Callable<File> {
    private static final Logger log = LoggerFactory.getLogger(DownloadCallable.class);

    private final COS cos;
    private final CountDownLatch latch;
    private final GetObjectRequest req;
    private final boolean resumeExistingDownload;
    private final DownloadImpl download;
    private final File dstfile;
    private final long origStartingByte;

    private long expectedFileLength;

    DownloadCallable(COS cos, CountDownLatch latch, GetObjectRequest req,
            boolean resumeExistingDownload, DownloadImpl download, File dstfile,
            long origStartingByte, long expectedFileLength) {
        if (cos == null || latch == null || req == null || dstfile == null || download == null)
            throw new IllegalArgumentException();
        this.cos = cos;
        this.latch = latch;
        this.req = req;
        this.resumeExistingDownload = resumeExistingDownload;
        this.download = download;
        this.dstfile = dstfile;
        this.origStartingByte = origStartingByte;
        this.expectedFileLength = expectedFileLength;
    }

    /**
     * This method must return a non-null object, or else the existing implementation in
     * {@link AbstractTransfer#waitForCompletion()} would block forever.
     * 
     * @return the downloaded file
     */
    @Override
    public File call() throws Exception {
        try {
            latch.await();
            download.setState(TransferState.InProgress);
            COSObject cosObject = retryableDownloadCOSObjectToFile(dstfile,
                    new DownloadTaskImpl(cos, download, req), resumeExistingDownload);

            if (cosObject == null) {
                download.setState(TransferState.Canceled);
                download.setMonitor(new DownloadMonitor(download, null));
            } else {
                download.setState(TransferState.Completed);
            }
            return dstfile;
        } catch (Throwable t) {
            // Downloads aren't allowed to move from canceled to failed
            if (download.getState() != TransferState.Canceled) {
                download.setState(TransferState.Failed);
            }
            if (t instanceof Exception)
                throw (Exception) t;
            else
                throw (Error) t;
        }
    }

    /**
     * This method is called only if it is a resumed download.
     *
     * Adjust the range of the get request, and the expected (ie current) file length of the
     * destination file to append to.
     */
    private void adjustRequest(GetObjectRequest req) {
        long[] range = req.getRange();
        long lastByte = range[1];
        long totalBytesToDownload = lastByte - this.origStartingByte + 1;

        if (dstfile.exists()) {
            if (!FileLocks.lock(dstfile)) {
                throw new FileLockException("Fail to lock " + dstfile + " for range adjustment");
            }
            try {
                expectedFileLength = dstfile.length();
                long startingByte = this.origStartingByte + expectedFileLength;
                log.info("Adjusting request range from " + Arrays.toString(range) + " to "
                        + Arrays.toString(new long[] {startingByte, lastByte}) + " for file "
                        + dstfile);
                req.setRange(startingByte, lastByte);
                totalBytesToDownload = lastByte - startingByte + 1;
            } finally {
                FileLocks.unlock(dstfile);
            }
        }

        if (totalBytesToDownload < 0) {
            throw new IllegalArgumentException(
                    "Unable to determine the range for download operation. lastByte=" + lastByte
                            + ", origStartingByte=" + origStartingByte + ", expectedFileLength="
                            + expectedFileLength + ", totalBytesToDownload="
                            + totalBytesToDownload);
        }
    }

    private COSObject retryableDownloadCOSObjectToFile(File file,
            RetryableCOSDownloadTask retryableCOSDownloadTask, boolean appendData) {
        boolean hasRetried = false;
        COSObject cosObject;
        for (;;) {
            if (resumeExistingDownload && hasRetried) {
                // Need to adjust the get range or else we risk corrupting the downloaded file
                adjustRequest(req);
            }
            cosObject = retryableCOSDownloadTask.getCOSObjectStream();
            if (cosObject == null)
                return null;
            try {
                if (testing && resumeExistingDownload && !hasRetried) {
                    throw new CosClientException("testing");
                }
                ServiceUtils.downloadToFile(cosObject, file,
                        retryableCOSDownloadTask.needIntegrityCheck(), appendData,
                        expectedFileLength);
                return cosObject;
            } catch (CosClientException ace) {
                if (!ace.isRetryable())
                    throw ace;
                // Determine whether an immediate retry is needed according to the captured
                // CosClientException.
                // (There are three cases when downloadObjectToFile() throws CosClientException:
                // 1) SocketException or SSLProtocolException when writing to disk (e.g. when user
                // aborts the download)
                // 2) Other IOException when writing to disk
                // 3) MD5 hashes don't match
                // The current code will retry the download only when case 2) or 3) happens.
                if (ace.getCause() instanceof SocketException
                        || ace.getCause() instanceof SSLProtocolException) {
                    throw ace;
                } else {
                    if (hasRetried)
                        throw ace;
                    else {
                        log.info("Retry the download of object " + cosObject.getKey() + " (bucket "
                                + cosObject.getBucketName() + ")", ace);
                        hasRetried = true;
                    }
                }
            } finally {
                cosObject.getObjectContent().abort();
            }
        }
    }

    private static boolean testing;

    /**
     * Used for testing purpose only.
     */
    static void setTesting(boolean b) {
        testing = b;
    }
}
