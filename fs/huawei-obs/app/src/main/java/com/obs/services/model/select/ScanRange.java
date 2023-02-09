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
 * Configuration of the scan range of the input data
 */
public class ScanRange extends XmlSerialization {
    private Long start;
    private Long end;

    /**
     * Sets the offset of the first byte of data to scan
     * 
     * Default is the beginning of the file
     * 
     * @param start
     *      Start offset of the scan range
     * 
     * @return Self
     */
    public ScanRange withStart(long start) {
        this.start = start;
        return this;
    }

    /**
     * Sets the offset of the last byte of data to scan
     * 
     * @param end
     *      Start offset of the scan range
     * 
     * @return Self
     */
    public ScanRange withEnd(long end) {
        this.end = end;
        return this;
    }

    /**
     * Formats the input settings into the XML request
     * 
     * @param xmlBuilder
     *              The xml serializer
     */
    @Override
    public void appendToXml(OBSXMLBuilder xmlBuilder) {
        OBSXMLBuilder rangeBuilder = xmlBuilder.elem("ScanRange");
        if (start != null) {
            rangeBuilder.elem("Start").text(start.toString());
        }
        if (end != null) {
            rangeBuilder.elem("End").text(end.toString());
        }
    }
}
