/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.select;

import com.obs.services.internal.xml.OBSXMLBuilder;

/**
 * Configuration of an OBS Select request
 */
public class SelectObjectRequest {
    private String bucketName;
    private String key;
    private String expression;
    private String expressionType;
    private RequestProgress requestProgress;
    private InputSerialization inputSerialization;
    private OutputSerialization outputSerialization;
    private ScanRange scanRange;

    /**
     * Returns the name of the bucket
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Returns the key of the input file
     * 
     * @return Key name
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the request (SQL) expression
     * 
     * @return Expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the name of the bucket
     * 
     * @param bucketName
     *      Bucket name
     * 
     * @return Self
     */
    public SelectObjectRequest withBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    /**
     * Sets the key of the input file
     * 
     * @param key
     *      Key of input file
     * 
     * @return Self
     */
    public SelectObjectRequest withKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Sets the (SQL) expression to be evaluated
     * 
     * @param expression
     *      Expression to evaluate
     * 
     * @return Self
     */
    public SelectObjectRequest withExpression(String expression) {
        this.expression = expression;
        return this;
    }

    /**
     * Informs that periodic request progress may be enabled
     * 
     * @param requestProgress
     *      Enable or disable requestProgress
     * 
     * @return Self
     */
    public SelectObjectRequest withRequestProgress(RequestProgress requestProgress) {
        this.requestProgress = requestProgress;
        return this;
    }

    /**
     * Informs the type of the expression.
     * 
     * Current only SQL is supported.
     * 
     * @param expressionType
     *      Type of the expression
     * 
     * @return Self
     */
    public SelectObjectRequest withExpressionType(ExpressionType expressionType) {
        this.expressionType = (expressionType == null ? null : expressionType.toString());
        return this;
    }

    /**
     * Configures the input serialization
     * 
     * @param inputSerialization
     *      Input serialization format
     * 
     * @return Self
     */
    public SelectObjectRequest withInputSerialization(InputSerialization inputSerialization) {
        this.inputSerialization = inputSerialization;
        return this;
    }

    /**
     * Configures the output serialization
     * 
     * @param inputSerialization
     *      Output serialization format
     * 
     * @return Self
     */
    public SelectObjectRequest withOutputSerialization(OutputSerialization outputSerialization) {
        this.outputSerialization = outputSerialization;
        return this;
    }

    /**
     * Configures the range of bytes of the input file to scan 
     * 
     * @param scanRange
     *      Range of bytes
     * 
     * @return Self
     */
    public SelectObjectRequest withScanRange(ScanRange scanRange) {
        this.scanRange = scanRange;
        return this;
    }

    /**
     * Generates the request in XML format
     * 
     * @return XML request
     */
    public String convertToXml() throws SelectObjectException {
        try {
            OBSXMLBuilder xmlBuilder = OBSXMLBuilder.create("SelectObjectContentRequest");
            if (expressionType != null) {
                xmlBuilder.elem("ExpressionType").text(expressionType);
            }
            if (expression != null) {
                xmlBuilder.elem("Expression").text(expression);
            }
            if (requestProgress != null) {
                requestProgress.appendToXml(xmlBuilder);
            }
            if (inputSerialization != null) {
                inputSerialization.appendToXml(xmlBuilder);
            }
            if (outputSerialization != null) {
                outputSerialization.appendToXml(xmlBuilder);
            }
            if (scanRange != null) {
                scanRange.appendToXml(xmlBuilder);
            }
            return xmlBuilder.asString();
        } catch (Exception e) {
            throw new SelectObjectException("Wrong request", e.getMessage());
        }
    }
}
