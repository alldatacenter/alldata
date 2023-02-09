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


package com.qcloud.cos.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;

import javax.net.ssl.SSLProtocolException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.FileLockException;
import com.qcloud.cos.internal.FileLocks;
import com.qcloud.cos.internal.SkipMd5CheckStrategy;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;

public class ServiceUtils {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);
    private static final SkipMd5CheckStrategy skipMd5CheckStrategy = SkipMd5CheckStrategy.INSTANCE;
    public static final boolean APPEND_MODE = true;
    public static final boolean OVERWRITE_MODE = false;
    /**
     * Same as {@link #downloadObjectToFile(COSObject, File, boolean, boolean)}
     * but has an additional expected file length parameter for integrity
     * checking purposes.
     *
     * @param expectedFileLength
     *            applicable only when appendData is true; the expected length
     *            of the file to append to.
     */
    public static void downloadToFile(COSObject cosObject,
        final File dstfile, boolean performIntegrityCheck,
        final boolean appendData,
        final long expectedFileLength)
    {
        // attempt to create the parent if it doesn't exist
        File parentDirectory = dstfile.getParentFile();
        if ( parentDirectory != null && !parentDirectory.exists() ) {
            if (!(parentDirectory.mkdirs())) {
                throw new CosClientException(
                        "Unable to create directory in the path"
                                + parentDirectory.getAbsolutePath());
            }
        }

        if (!FileLocks.lock(dstfile)) {
            throw new FileLockException("Fail to lock " + dstfile
                    + " for appendData=" + appendData);
        }
        OutputStream outputStream = null;
        try {
            final long actualLen = dstfile.length();
            if (appendData && actualLen != expectedFileLength) {
                // Fail fast to prevent data corruption
                throw new IllegalStateException(
                        "Expected file length to append is "
                            + expectedFileLength + " but actual length is "
                            + actualLen + " for file " + dstfile);
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(
                    dstfile, appendData));
            byte[] buffer = new byte[1024*10];
            int bytesRead;
            while ((bytesRead = cosObject.getObjectContent().read(buffer)) > -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            cosObject.getObjectContent().abort();
            throw new CosClientException(
                    "Unable to store object contents to disk: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(outputStream, log);
            FileLocks.unlock(dstfile);
            IOUtils.closeQuietly(cosObject.getObjectContent(), log);
        }

        if (performIntegrityCheck) {
            byte[] clientSideHash = null;
            byte[] serverSideHash = null;
            try {
                final ObjectMetadata metadata = cosObject.getObjectMetadata();
                if (!skipMd5CheckStrategy.skipClientSideValidationPerGetResponse(metadata)) {
                    clientSideHash = Md5Utils.computeMD5Hash(dstfile);
                    serverSideHash = BinaryUtils.fromHex(metadata.getETag());
                }
            } catch (Exception e) {
                log.warn("Unable to calculate MD5 hash to validate download: " + e.getMessage(), e);
            }

            if (clientSideHash != null && serverSideHash != null && !Arrays.equals(clientSideHash, serverSideHash)) {
                throw new CosClientException("Unable to verify integrity of data download.  " +
                        "Client calculated content hash didn't match hash calculated by Qcloud COS.  " +
                        "The data stored in '" + dstfile.getAbsolutePath() + "' may be corrupt.");
            }
        }
    }
    
    /**
     * Downloads an COSObject, as returned from
     * {@link COSClient#getObject(com.qcloud.cos.model.GetObjectRequest)},
     * to the specified file.
     *
     * @param cosObject
     *            The COSObject containing a reference to an InputStream
     *            containing the object's data.
     * @param destinationFile
     *            The file to store the object's data in.
     * @param performIntegrityCheck
     *            Boolean valuable to indicate whether to perform integrity check
     * @param appendData
     *            appends the data to end of the file.
     */
    public static void downloadObjectToFile(COSObject cosObject,
            final File destinationFile, boolean performIntegrityCheck,
            boolean appendData) {
        downloadToFile(cosObject, destinationFile, performIntegrityCheck, appendData, -1);
    }
    
    /**
     * Interface for the task of downloading object from COS to a specific file,
     * enabling one-time retry mechanism after integrity check failure
     * on the downloaded file.
     */
    public interface RetryableCOSDownloadTask {
        /**
         * User defines how to get the COSObject from COS for this RetryableCOSDownloadTask.
         *
         * @return
         *         The COSObject containing a reference to an InputStream
         *        containing the object's data.
         */
        public COSObject getCOSObjectStream ();
        /**
         * User defines whether integrity check is needed for this RetryableCOSDownloadTask.
         *
         * @return
         *         Boolean value indicating whether this task requires integrity check
         *         after downloading the COS object to file.
         */
        public boolean needIntegrityCheck ();
    }

    /**
     * Gets an object stored in COS and downloads it into the specified file.
     * This method includes the one-time retry mechanism after integrity check failure
     * on the downloaded file. It will also return immediately after getting null valued
     * COSObject (when getObject request does not meet the specified constraints).
     *
     * @param file
     *             The file to store the object's data in.
     * @param retryableCOSDownloadTask
     *             The implementation of SafeCOSDownloadTask interface which allows user to
     *             get access to all the visible variables at the calling site of this method.
     */
    public static COSObject retryableDownloadCOSObjectToFile(File file,
            RetryableCOSDownloadTask retryableCOSDownloadTask, boolean appendData) {
        boolean hasRetried = false;
        boolean needRetry;
        COSObject cosObject;
        do {
            needRetry = false;
            cosObject = retryableCOSDownloadTask.getCOSObjectStream();
            if ( cosObject == null )
                return null;

            try {
                ServiceUtils.downloadObjectToFile(cosObject, file,
                        retryableCOSDownloadTask.needIntegrityCheck(),
                        appendData);
            } catch (CosClientException cse) {
                if (!cse.isRetryable()) {
                    cosObject.getObjectContent().abort();
                    throw cse;
                }
                // Determine whether an immediate retry is needed according to the captured CosClientException.
                // (There are three cases when downloadObjectToFile() throws CosClientException:
                //        1) SocketException or SSLProtocolException when writing to disk (e.g. when user aborts the download)
                //        2) Other IOException when writing to disk
                //        3) MD5 hashes don't match
                // The current code will retry the download only when case 2) or 3) happens.
                if (cse.getCause() instanceof SocketException || cse.getCause() instanceof SSLProtocolException) {
                    throw cse;
                } else {
                    needRetry = true;
                    if ( hasRetried ) {
                        cosObject.getObjectContent().abort();
                        throw cse;
                    } else {
                        log.info("Retry the download of object " + cosObject.getKey() + " (bucket " + cosObject.getBucketName() + ")", cse);
                        hasRetried = true;
                    }
                }
            }
        } while ( needRetry );
        return cosObject;
    }

}
