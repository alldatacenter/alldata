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

public class OutputSerialization extends XmlSerialization {
    private XmlSerialization output;

    /**
     * Declares the output serialization format as CSV
     * 
     * @param csvOutput
     *          Serialization format for a CSV output file
     * 
     * @return Self
     */
    public OutputSerialization withCsv(CsvOutput csvOutput) {
        this.output = csvOutput;
        return this;
    }

    /**
     * Declares the input serialization format as JSON
     * 
     * @param jsonOutput
     *          Serialization format for a JSON output file
     * 
     * @return Self
     */
    public OutputSerialization withJson(JsonOutput jsonOutput) {
        this.output = jsonOutput;
        return this;
    }

    /**
     * Declares the input serialization format as Arrow
     *
     *
     * @return Self
     */
    public OutputSerialization withArrow() {
        this.output = new ArrowOutput();
        return this;
    }

    /**
     * Formats the output settings into the XML request
     * 
     * @param xmlBuilder
     *              The xml serializer
     */
    @Override
    void appendToXml(OBSXMLBuilder xmlBuilder) {
        OBSXMLBuilder inputBuilder = xmlBuilder.elem("OutputSerialization");
        if (output != null) {
            output.appendToXml(inputBuilder);
        }
    }
}
