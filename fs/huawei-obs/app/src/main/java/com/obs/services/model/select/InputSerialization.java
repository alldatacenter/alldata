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
 * Input serialization format
 */
public class InputSerialization extends XmlSerialization {
    private CompressionType compressionType;
    private XmlSerialization input;

    /**
     * Compression type of the input file
     * 
     * @param compressionType
     *          Compression type format, default is NONE
     * 
     * @return Self
     */
    public InputSerialization withCompressionType(CompressionType compressionType) {
        this.compressionType = compressionType;
        return this;
    }

    /**
     * Declares the input serialization format as CSV
     * 
     * @param csvInput
     *          Serialization format for a CSV input file
     * 
     * @return Self
     */
    public InputSerialization withCsv(CsvInput csvInput) {
        this.input = csvInput;
        return this;
    }

    /**
     * Declares the input serialization format as JSON
     * 
     * @param jsonInput
     *          Serialization format for a JSON input file
     * 
     * @return Self
     */
    public InputSerialization withJson(JsonInput jsonInput) {
        this.input = jsonInput;
        return this;
    }

    /**
     * Declares the input serialization format as ORC
     * 
     * @param orcInput
     *          Serialization format for an ORC input file
     * 
     * @return Self
     */
    public InputSerialization withOrc(OrcInput orcInput) {
        this.input = orcInput;
        return this;
    }

    /**
     * Declares the input serialization format as Parquet
     * 
     * @param parquetInput
     *          Serialization format for a Parquet input file
     * 
     * @return Self
     */
    public InputSerialization withParquet(ParquetInput parquetInput) {
        this.input = parquetInput;
        return this;
    }

    /**
     * Formats the input settings into the XML request
     * 
     * @param xmlBuilder
     *              The xml serializer
     */
    @Override
    void appendToXml(OBSXMLBuilder xmlBuilder) {
        OBSXMLBuilder inputBuilder = xmlBuilder.elem("InputSerialization");
        if (compressionType != null) {
            inputBuilder.elem("CompressionType").text(compressionType.toString());
        }
        if (input != null) {
            input.appendToXml(inputBuilder);
        }
    }
}
