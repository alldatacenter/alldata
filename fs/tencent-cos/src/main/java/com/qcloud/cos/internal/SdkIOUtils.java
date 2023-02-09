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


package com.qcloud.cos.internal;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdkIOUtils {
    private static final Logger defaultLog = LoggerFactory.getLogger(SdkIOUtils.class);

    public static void closeQuietly(Closeable is) {
        closeQuietly(is, null);
    }

    /**
     * Closes the given Closeable quietly.
     * 
     * @param is the given closeable
     * @param log logger used to log any failure should the close fail
     */
    public static void closeQuietly(Closeable is, Logger log) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                Logger logger = log == null ? defaultLog : log;
                if (logger.isDebugEnabled())
                    logger.debug("Ignore failure in closing the Closeable", ex);
            }
        }
    }
}
