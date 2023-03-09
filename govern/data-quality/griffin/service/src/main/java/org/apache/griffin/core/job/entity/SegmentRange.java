/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.griffin.core.measure.entity.AbstractAuditableEntity;

@Entity
public class SegmentRange extends AbstractAuditableEntity {

    private static final long serialVersionUID = -8929713841303669564L;

    @Column(name = "data_begin")
    private String begin = "-1h";

    private String length = "1h";

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public SegmentRange(String begin, String length) {
        this.begin = begin;
        this.length = length;
    }

    SegmentRange() {
    }

}
