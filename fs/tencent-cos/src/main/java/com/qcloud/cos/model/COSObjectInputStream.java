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


package com.qcloud.cos.model;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.EofSensorInputStream;

import com.qcloud.cos.internal.SdkFilterInputStream;
import com.qcloud.cos.utils.IOUtils;

/**
 * Input stream representing the content of an {@link COSObject}. In addition to
 * the methods supplied by the {@link InputStream} class,
 * {@link COSObjectInputStream} supplies the abort() method, which will terminate
 * an HTTP connection to the COS object.
 */
public class COSObjectInputStream extends SdkFilterInputStream {

    private final HttpRequestBase httpRequest;

    private boolean eof;

    public COSObjectInputStream(
            InputStream in,
            HttpRequestBase httpRequest) {

        super(in);

        this.httpRequest = httpRequest;
    }

    /**
     * {@inheritDoc}
     *
     * Aborts the underlying http request without reading any more data and
     * closes the stream.
     * <p>
     * By default Apache {@link HttpClient} tries to reuse http connections by
     * reading to the end of an attached input stream on
     * {@link InputStream#close()}. This is efficient from a socket pool
     * management perspective, but for objects with large payloads can incur
     * significant overhead while bytes are read from COS and discarded. It's up
     * to clients to decide when to take the performance hit implicit in not
     * reusing an http connection in order to not read unnecessary information
     * from COS.
     *
     * @see EofSensorInputStream
     */
    @Override
    public void abort() {
        doAbort();
    }

    /**
     * To allow customers to override abort to just close. We can think about exposing this method
     * as protected to allow customers to completely prevent the abort behavior if there is a need
     */
    private void doAbort() {
        if (httpRequest != null) {
            httpRequest.abort();
        }
        IOUtils.closeQuietly(in, null);
    }

    /**
     * Returns the http request from which this input stream is derived.
     */
    public HttpRequestBase getHttpRequest() {
        return httpRequest;
    }

    /**
     * Returns super.available() if the value is not zero or else always returns
     * 1.  This is necessary to get around a GZIPInputStream bug which would
     * mis-behave in some edge cases upon zero returned from available(),
     * causing file truncation.
     * <p>
     * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7036144
     * <p>
     * Reference TT: 0034867351
     */
    @Override
    public int available() throws IOException {
        int estimate = super.available();
        return estimate == 0 ? 1 : estimate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value == -1) {
            eof = true;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int value = super.read(b, off, len);
        if (value == -1) {
            eof = true;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        super.reset();
        eof = false;
    }

    /**
     * {@inheritDoc}
     *
     * Delegate to {@link COSObjectInputStream#abort()} if there is data remaining in the stream. If the stream has been
     * read completely, with no data remaining, safely close the stream.
     *
     * @see {@link COSObjectInputStream#abort()}
     */
    @Override
    public void close() throws IOException {
        if (eof) {
            super.close();
            httpRequest.releaseConnection();
        } else {
            doAbort();
        }
    }
}

