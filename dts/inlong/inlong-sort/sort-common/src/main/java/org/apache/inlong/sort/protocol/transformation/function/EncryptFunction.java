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

package org.apache.inlong.sort.protocol.transformation.function;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.CascadeFunction;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;

/**
 * EncryptFunction class is the logic encapsulation of String encryption
 */
@JsonTypeName("encrypt")
@EqualsAndHashCode(callSuper = false)
@Data
public class EncryptFunction implements CascadeFunction, Serializable {

    private static final long serialVersionUID = -2701547146694616429L;

    @JsonProperty("field")
    private FieldInfo field;
    @JsonProperty("key")
    private StringConstantParam key;
    @JsonProperty("encrypt")
    private StringConstantParam encrypt;

    /**
     * EncryptFunction constructor
     *
     * @param field it is character to be encrypted
     * @param key the key of encryption
     * @param encrypt encryption algorithm
     */
    @JsonCreator
    public EncryptFunction(@JsonProperty("field") FieldInfo field,
            @JsonProperty("key") StringConstantParam key,
            @JsonProperty("encrypt") StringConstantParam encrypt) {
        this.field = Preconditions.checkNotNull(field, "field is null");
        this.key = Preconditions.checkNotNull(key, "key is null");
        this.encrypt = Preconditions.checkNotNull(encrypt, "encrypt is null");
    }

    @Override
    public String getName() {
        return "ENCRYPT";
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(field, key, encrypt);
    }

    @Override
    public String format() {
        return String.format("%s(CAST(%s AS STRING), %s, %s)",
                getName(), field.format(), key.format(), encrypt.format());
    }

    @Override
    public ConstantParam apply(ConstantParam constantParam) {
        return new ConstantParam(String.format("%s(%s, %s, %s)", getName(),
                constantParam.format(), key.format(), encrypt.format()));
    }
}
