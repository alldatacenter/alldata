/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.base.dirty.sink.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * S3 helper class, it helps write to s3
 */
public class S3Helper implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(S3DirtySink.class);

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final int SEQUENCE_LENGTH = 4;
    private static final String ESCAPE_PATTERN = "[，,+=: ;()（）。/.；]";
    private static final String FILE_NAME_SUFFIX = ".txt";
    private final Random r = new Random();
    private final AmazonS3 s3Client;
    private final S3Options s3Options;

    S3Helper(AmazonS3 s3Client, S3Options s3Options) {
        this.s3Client = s3Client;
        this.s3Options = s3Options;
    }

    /**
     * Upload data to s3
     *
     * @param identifier The identifier of dirty data
     * @param content The content that will be upload
     * @throws IOException The exception may be thrown when executing
     */
    public void upload(String identifier, String content) throws IOException {
        String path = genFileName(identifier);
        for (int i = 0; i < s3Options.getMaxRetries(); i++) {
            try {
                s3Client.putObject(s3Options.getBucket(), path, content);
                break;
            } catch (Exception e) {
                LOG.error("s3 dirty sink error, retry times = {}", i, e);
                if (i >= s3Options.getMaxRetries()) {
                    throw new IOException(e);
                }
                try {
                    Thread.sleep(1000L * i);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("unable to flush; interrupted while doing another attempt", e);
                }
            }
        }
    }

    /**
     * Generate the file name for s3
     *
     * @param identifier The identifier of dirty data
     * @return File name of s3
     */
    private String genFileName(String identifier) {
        return String.format("%s/%s-%s%s", s3Options.getKey(),
                identifier.replaceAll(ESCAPE_PATTERN, ""), generateSequence(), FILE_NAME_SUFFIX);
    }

    private String generateSequence() {
        StringBuilder sb = new StringBuilder(DATE_TIME_FORMAT.format(LocalDateTime.now()));
        for (int i = 0; i < SEQUENCE_LENGTH; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

}
