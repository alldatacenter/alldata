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
 * Auxiliary methods for serialization to a XML request
 */
public abstract class XmlSerialization {
    /**
     * Formats the content into a XML request document compliant with the AWS S3 SelectObjectContent
     * specification defined in https://docs.aws.amazon.com/AmazonS3/latest/API/API_SelectObjectContent.html
     * 
     * @param xmlBuilder
     *              The xml serializer
     */
    abstract void appendToXml(OBSXMLBuilder xmlBuilder);

    /**
     * Converts a special character to its proper XML string representation
     * 
     * @param ch
     *      Character to convert
     * 
     * @return The valid XML string for the character
     */
    protected String charToString(Character ch) {
        if (ch == '\n') {
            return "\\n";
        }
        if (ch == '\r') {
            return "\\r";
        }
        if (ch == '\t') {
            return "\\t";
        }
        if (ch == '\f') {
            return "\\f";
        }
        return ch.toString();
    }
}
