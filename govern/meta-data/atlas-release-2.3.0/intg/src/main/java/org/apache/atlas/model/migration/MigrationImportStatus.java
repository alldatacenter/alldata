/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.model.migration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.impexp.MigrationStatus;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MigrationImportStatus extends MigrationStatus {
    private String name;
    private String fileHash;

    public MigrationImportStatus() {
    }

    public MigrationImportStatus(String name) {
        this.name     = name;
        this.fileHash = name;
    }

    public MigrationImportStatus(String name, String fileHash) {
        this.name     = name;
        this.fileHash = fileHash;
    }

    public String getName() {
        return name;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MigrationImportStatus{");
        sb.append("name='").append(name).append('\'');
        sb.append(", fileHash='").append(fileHash).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
