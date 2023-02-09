/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.qcloud.cos.utils.Jackson;

public class BucketReplicationConfiguration implements Serializable {

    private String roleName;

    /** Collection of replication rules associated with the QCloud bucket. */
    private List<ReplicationRule> rules = new LinkedList<>();

    /**
     * Returns the role name associated with this replication configuration.
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Sets the role name that will be used by QCloud while replication.
     *
     * @param role The role name for this configuration.
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * Sets the role name that will be used by QCloud while replication. Returns the updated object.
     *
     * @param roleName The role for this configuration.
     * @return The updated {@link BucketReplicationConfiguration} object.
     */
    public BucketReplicationConfiguration withRoleName(String roleName) {
        setRoleName(roleName);
        return this;
    }

    /**
     * Returns the replication rules associated with this QCloud bucket.
     *
     * @return the replication rules associated with this QCloud bucket.
     */
    public List<ReplicationRule> getRules() {
        return rules;
    }

    /**
     * Sets the replication rules for the QCloud bucket.
     *
     * @param rules the replication rules for the QCloud bucket.
     * @throws IllegalArgumentException if the rules are null.
     */
    public void setRules(List<ReplicationRule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Replication rules cannot be null");
        }
        this.rules = new LinkedList<ReplicationRule>(rules);
    }

    /**
     * Sets the replication rules for the QCloud bucket. Returns the updated object.
     *
     * @param rules the replication rules for the QCloud bucket.
     * @throws IllegalArgumentException if the rules are null.
     * @return the updated {@link BucketReplicationConfiguration} object.
     */
    public BucketReplicationConfiguration withRules(List<ReplicationRule> rules) {
        setRules(rules);
        return this;
    }

    /**
     * Adds a new rule to the replication configuration associated with this QCloud bucket. Returns
     * the updated object.
     *
     * @param id the id for the rule.
     * @param rule the replication rule for the QCloud bucket.
     * @throws IllegalArgumentException if the given id or rule is null.
     * @return the updated {@link BucketReplicationConfiguration} object.
     */
    public BucketReplicationConfiguration addRule(ReplicationRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Replication rule cannot be null");
        }
        rules.add(rule);
        return this;
    }

    /**
     * Removes the replication rule with the given id from the replication configuration associated
     * with QCloud bucket. Returns the updated object.
     *
     * @param id the id of the replication rule to be removed.
     * @return the updated {@link BucketReplicationConfiguration} object.
     */
    public BucketReplicationConfiguration removeRule(ReplicationRule rule) {
        rules.remove(rule);
        return this;
    }

    @Override
    public String toString() {
        return Jackson.toJsonString(this);
    }
}
