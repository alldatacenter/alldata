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

package org.apache.inlong.sort.protocol.node.extract;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.constant.DorisConstant;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * doris extract node using doris flink-doris-connector-1.13.5_2.11
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("dorisExtract")
@Data
public class DorisExtractNode extends ExtractNode implements Serializable {

    private static final long serialVersionUID = -1369223293553991653L;

    @JsonProperty("feNodes")
    @Nonnull
    private String feNodes;

    @JsonProperty("username")
    @Nonnull
    private String userName;

    @JsonProperty("password")
    @Nonnull
    private String password;

    @JsonProperty("tableIdentifier")
    @Nonnull
    private String tableIdentifier;

    @JsonCreator
    public DorisExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField waterMarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("feNodes") @Nonnull String feNodes,
            @JsonProperty("username") String userName,
            @JsonProperty("password") String password,
            @JsonProperty("tableIdentifier") String tableIdentifier) {
        super(id, name, fields, waterMarkField, properties);
        this.feNodes = Preconditions.checkNotNull(feNodes, "feNodes is null");
        this.userName = Preconditions.checkNotNull(userName, "username is null");
        this.password = Preconditions.checkNotNull(password, "password is null");
        this.tableIdentifier = Preconditions.checkNotNull(tableIdentifier, "tableIdentifier is null");
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put(DorisConstant.CONNECTOR, "doris");
        options.put(DorisConstant.FE_NODES, feNodes);
        options.put(DorisConstant.USERNAME, userName);
        options.put(DorisConstant.PASSWORD, password);
        options.put(DorisConstant.TABLE_IDENTIFIER, tableIdentifier);

        return options;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }

}
