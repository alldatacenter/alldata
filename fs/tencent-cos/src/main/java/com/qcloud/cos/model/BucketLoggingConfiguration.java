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
import java.io.Serializable;


public class BucketLoggingConfiguration implements Serializable {
    private String destinationBucketName = null;
    private String logFilePrefix = null;

    /**
     * Creates a new bucket logging configuration, which by default is
     * <b>disabled</b>.
     * <p>
     * Passing this new object directly to
     * {@link com.qcloud.cos.COSClient#setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest)}
     * will turn off bucket logging for the specified bucket.
     * </p>
     */
    public BucketLoggingConfiguration() {}

    /**
     * Creates a new bucket logging configuration which enables server access
     * logs to be collected and stored in the specified destination bucket with
     * the specified log file prefix.
     *
     * @param destinationBucketName
     *            The name of the bucket to which to delivery server access logs
     *            from the target bucket. This may be the same bucket for which
     *            logging is being configured.
     * @param logFilePrefix
     *            The optional prefix to append to server access logs when they
     *            are written to the destination bucket.
     */
    public BucketLoggingConfiguration(String destinationBucketName, String logFilePrefix) {
        setLogFilePrefix(logFilePrefix);
        setDestinationBucketName(destinationBucketName);
    }

    /**
     * Returns true if logging is enabled.
     *
     * @return True if logging is enabled.
     */
    public boolean isLoggingEnabled() {
        return destinationBucketName != null
                && logFilePrefix != null;
    }

    /**
     * Returns the optional log file prefix.
     *
     * @return The optional log file prefix.
     */
    public String getLogFilePrefix() {
        return logFilePrefix;
    }

    /**
     * Sets the log file prefix for this bucket logging configuration.
     *
     * @param logFilePrefix The log file prefix for this logging configuration.
     */
    public void setLogFilePrefix(String logFilePrefix) {
        // Default log file prefix to the empty string if none is specified
        if (logFilePrefix == null)
            logFilePrefix = "";

        this.logFilePrefix = logFilePrefix;
    }

    /**
     * Returns the destination bucket name for this logging configuration.
     *
     * @return The destination bucket name for this logging configuration.
     */
    public String getDestinationBucketName() {
        return destinationBucketName;
    }

    /**
     * Sets the destination bucket name for this logging configuration.
     *
     * @param destinationBucketName The destination bucket name for this logging configuration.
     */
    public void setDestinationBucketName(String destinationBucketName) {
        this.destinationBucketName = destinationBucketName;
    }

    public String toString() {
        String result = "LoggingConfiguration enabled=" + isLoggingEnabled();
        if (isLoggingEnabled()) {
            result += ", destinationBucketName=" + getDestinationBucketName()
                    + ", logFilePrefix=" + getLogFilePrefix();
        }
        return result;
    }

}
