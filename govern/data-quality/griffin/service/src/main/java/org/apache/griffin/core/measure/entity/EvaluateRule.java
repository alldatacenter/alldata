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

package org.apache.griffin.core.measure.entity;


import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;


@Entity
public class EvaluateRule extends AbstractAuditableEntity {
    private static final long serialVersionUID = 4240072518233967528L;

    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST,
        CascadeType.REMOVE, CascadeType.MERGE})
    @JoinColumn(name = "evaluate_rule_id")
    @OrderBy("id ASC")
    private List<Rule> rules = new ArrayList<>();

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public EvaluateRule() {
    }

    public EvaluateRule(List<Rule> rules) {
        this.rules = rules;
    }
}

