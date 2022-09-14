/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo;

import java.util.List;

/**
 * View object
 */
public class JobConfValueVo {
    /**
     * Config key id
     */
    private Long configkeyId;
    /**
     * Key
     */
    private String key;
    /**
     * Name
     */
    private String name;
    /**
     * Value
     */
    private String value;
    /**
     * Value list
     */
    private List<ValueList> valueLists;

    public JobConfValueVo(){

    }

    public JobConfValueVo(String key, String value){
        this.key = key;
        this.value = value;
    }

    public static class ValueList {
        private String value;
        private Boolean selected;

        public ValueList() {
        }

        public ValueList(String value, Boolean selected) {
            this.value = value;
            this.selected = selected;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Boolean getSelected() {
            return selected;
        }

        public void setSelected(Boolean selected) {
            this.selected = selected;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ValueList> getValueLists() {
        return valueLists;
    }

    public void setValueLists(List<ValueList> valueLists) {
        this.valueLists = valueLists;
    }

    public Long getConfigkeyId() {
        return configkeyId;
    }

    public void setConfigkeyId(Long configkeyId) {
        this.configkeyId = configkeyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
