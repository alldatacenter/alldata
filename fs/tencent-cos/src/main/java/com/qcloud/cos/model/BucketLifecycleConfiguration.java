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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.qcloud.cos.model.lifecycle.LifecycleFilter;


/**
 * Container for bucket lifecycle configuration operations.
 */
public class BucketLifecycleConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constant for an enabled rule.
     *
     * @see Rule#setStatus(String)
     */
    public static final String ENABLED = "Enabled";

    /**
     * Constant for a disabled rule.
     *
     * @see Rule#setStatus(String)
     */
    public static final String DISABLED = "Disabled";

    private List<Rule> rules;

    /**
     * Returns the list of rules that comprise this configuration.
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Sets the rules that comprise this configuration.
     */
    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Sets the rules that comprise this configuration and returns a reference to this object for
     * easy method chaining.
     */
    public BucketLifecycleConfiguration withRules(List<Rule> rules) {
        setRules(rules);
        return this;
    }

    /**
     * Convenience array style method for {@link BucketLifecycleConfiguration#withRules(List)}
     */
    public BucketLifecycleConfiguration withRules(Rule... rules) {
        setRules(Arrays.asList(rules));
        return this;
    }

    /**
     * Constructs a new {@link BucketLifecycleConfiguration} object with the rules given.
     *
     * @param rules
     */
    public BucketLifecycleConfiguration(List<Rule> rules) {
        this.rules = rules;
    }

    public BucketLifecycleConfiguration() {
        super();
    }

    public static class Rule  implements Serializable {

        private static final long serialVersionUID = 1L;
        private String id;
        private String status;
        private LifecycleFilter filter;

        /**
         * The time, in days, between when the object is uploaded to the bucket and when it expires.
         * Should not coexist with expirationDate within one lifecycle rule.
         */
        private int expirationInDays = -1;

        private boolean expiredObjectDeleteMarker = false;

        /**
         * The time, in days, between when a new version of the object is uploaded to the bucket and
         * when older versions of the object expire.
         */
        private int noncurrentVersionExpirationInDays = -1;

        /**
         * The expiration date of the object and should not coexist with expirationInDays within one
         * lifecycle rule.
         */
        private Date expirationDate;
        private List<Transition> transitions;
        /**
         * Transition rules for the non current objects in the bucket.
         */
        private List<NoncurrentVersionTransition> noncurrentVersionTransitions;

        /**
         * Specifies the days since the initiation of an Incomplete Multipart Upload that Lifecycle
         * will wait before permanently removing all parts of the upload.
         */
        private AbortIncompleteMultipartUpload abortIncompleteMultipartUpload;

        /**
         * Sets the ID of this rule. Rules must be less than 255 alphanumeric characters, and must
         * be unique for a bucket. If you do not assign an ID, one will be generated.
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Sets the time, in days, between when an object is uploaded to the bucket and when it
         * expires.
         */
        public void setExpirationInDays(int expirationInDays) {
            this.expirationInDays = expirationInDays;
        }

        /**
         * Returns the ID of this rule.
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the ID of this rule and returns a reference to this object for method chaining.
         *
         * @see Rule#setId(String)
         */
        public Rule withId(String id) {
            this.id = id;
            return this;
        }

        /**
         * Returns the time in days from an object's creation to its expiration.
         */
        public int getExpirationInDays() {
            return expirationInDays;
        }

        /**
         * Sets the time, in days, between when an object is uploaded to the bucket and when it
         * expires, and returns a reference to this object for method chaining.
         *
         * @see Rule#setExpirationInDays(int)
         */
        public Rule withExpirationInDays(int expirationInDays) {
            this.expirationInDays = expirationInDays;
            return this;
        }


        /**
         * Returns whether the current expiration policy for the object is set to remove objects
         * when only a delete marker is left
         * <p>
         * If set to true the lifecycle policy will delete the current version of an object if and
         * only if the current version is a expired object delete marker. This option only makes
         * sense to use for versioned buckets and cannot be used in conjunction with
         * expirationInDays or expirationDate. Note that the current version can only be removed if
         * all non-current versions have been removed (either through a non-current version
         * expiration policy or being explicitly deleted)
         * </p>
         *
         * @return True if this lifecycle's configuration is configured to delete the current
         *         version of an object if it's the only version left and it's a delete marker.
         *         False otherwise
         */
        public boolean isExpiredObjectDeleteMarker() {
            return expiredObjectDeleteMarker;
        }

        /**
         * Sets the value of the ExpiredObjectDeleteMarkers attribute.
         *
         * @param expiredObjectDeleteMarker True to allow the current expiration policy to remove
         *        the current version of the object if it's the only version left and it's a delete
         *        marker. False has no effect on the current expiration policy
         */
        public void setExpiredObjectDeleteMarker(boolean expiredObjectDeleteMarker) {
            this.expiredObjectDeleteMarker = expiredObjectDeleteMarker;
        }

        /**
         * Fluent method for setting the value of the ExpiredObjectDeleteMarkers attributes. See
         * {@link #setExpiredObjectDeleteMarker(boolean)}
         *
         * @param expiredObjectDeleteMarker
         * @return This object for method chaining
         */
        public Rule withExpiredObjectDeleteMarker(boolean expiredObjectDeleteMarker) {
            this.expiredObjectDeleteMarker = expiredObjectDeleteMarker;
            return this;
        }

        public int getNoncurrentVersionExpirationInDays() {
            return noncurrentVersionExpirationInDays;
        }

        public void setNoncurrentVersionExpirationInDays(int noncurrentVersionExpirationInDays) {
            this.noncurrentVersionExpirationInDays = noncurrentVersionExpirationInDays;
        }

        public Rule withNoncurrentVersionExpirationInDays(int noncurrentVersionExpirationInDays) {
            this.noncurrentVersionExpirationInDays = noncurrentVersionExpirationInDays;
            return this;
        }

        /**
         * Returns the status of this rule.
         *
         * @see BucketLifecycleConfiguration#DISABLED
         * @see BucketLifecycleConfiguration#ENABLED
         */
        public String getStatus() {
            return status;
        }

        /**
         * Sets the status of this rule.
         *
         * @see BucketLifecycleConfiguration#DISABLED
         * @see BucketLifecycleConfiguration#ENABLED
         */
        public void setStatus(String status) {
            this.status = status;
        }

        /**
         * Sets the status of this rule and returns a reference to this object for method chaining.
         *
         * @see Rule#setStatus(String)
         * @see BucketLifecycleConfiguration#DISABLED
         * @see BucketLifecycleConfiguration#ENABLED
         */
        public Rule withStatus(String status) {
            setStatus(status);
            return this;
        }

        /**
         * Sets the expiration date of the object.
         */
        public void setExpirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
        }

        /**
         * Returns the expiration date of the object.
         */
        public Date getExpirationDate() {
            return this.expirationDate;
        }

        /**
         * Sets the expiration date of the object and returns a reference to this object(Rule) for
         * method chaining.
         */
        public Rule withExpirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        /**
         * Sets the transition describing how this object will move between different storage
         * classes in Qcloud COS.
         */
        public void setTransitions(List<Transition> transitions) {
            this.transitions = new ArrayList<Transition>(transitions);
        }

        /**
         * Returns the transition attribute of the rule.
         */
        public List<Transition> getTransitions() {
            return this.transitions;
        }

        /**
         * Sets the transition describing how this object will move between different storage
         * classes in Qcloud COS and returns a reference to this object(Rule) for method chaining.
         */
        public Rule withTransitions(List<Transition> transitions) {
            this.transitions = transitions;
            return this;
        }

        /**
         * Adds a new transition to the rule.
         */
        public Rule addTransition(Transition transition) {
            if (transition == null) {
                throw new IllegalArgumentException("Transition cannot be null.");
            }

            if (transitions == null) {
                transitions = new ArrayList<BucketLifecycleConfiguration.Transition>();
            }
            transitions.add(transition);
            return this;
        }

        /**
         * Returns the Qcloud COS non current object transition rules associated with the given
         * rule.
         */
        public List<NoncurrentVersionTransition> getNoncurrentVersionTransitions() {
            return noncurrentVersionTransitions;
        }

        /**
         * Sets the Qcloud COS non current object transition rules for the given bucket.
         */
        public void setNoncurrentVersionTransitions(
                List<NoncurrentVersionTransition> noncurrentVersionTransitions) {
            this.noncurrentVersionTransitions =
                    new ArrayList<NoncurrentVersionTransition>(noncurrentVersionTransitions);
        }

        /**
         * Sets the Qcloud COS non current object transition rules for the given bucket. Returns an
         * updated version of this object.
         */
        public Rule withNoncurrentVersionTransitions(
                List<NoncurrentVersionTransition> noncurrentVersionTransitions) {
            setNoncurrentVersionTransitions(noncurrentVersionTransitions);
            return this;
        }

        /**
         * Adds a new Non current transition to the rule.
         */
        public Rule addNoncurrentVersionTransition(
                NoncurrentVersionTransition noncurrentVersionTransition) {
            if (noncurrentVersionTransition == null) {
                throw new IllegalArgumentException("NoncurrentVersionTransition cannot be null.");
            }

            if (noncurrentVersionTransitions == null) {
                noncurrentVersionTransitions =
                        new ArrayList<BucketLifecycleConfiguration.NoncurrentVersionTransition>();
            }
            noncurrentVersionTransitions.add(noncurrentVersionTransition);
            return this;
        }

        public AbortIncompleteMultipartUpload getAbortIncompleteMultipartUpload() {
            return abortIncompleteMultipartUpload;
        }

        public void setAbortIncompleteMultipartUpload(
                AbortIncompleteMultipartUpload abortIncompleteMultipartUpload) {
            this.abortIncompleteMultipartUpload = abortIncompleteMultipartUpload;
        }

        public Rule withAbortIncompleteMultipartUpload(
                AbortIncompleteMultipartUpload abortIncompleteMultipartUpload) {
            setAbortIncompleteMultipartUpload(abortIncompleteMultipartUpload);
            return this;
        }

        /**
         * Returns a {@link LifecycleFilter} that is used to identify objects that a Lifecycle Rule
         * applies to.
         */
        public LifecycleFilter getFilter() {
            return filter;
        }

        /**
         * Sets the {@link LifecycleFilter} that is used to identify objects that a Lifecycle Rule
         * applies to. A rule cannot have both {@link LifecycleFilter} and the deprecated
         * {@link #prefix}.
         *
         * @param filter {@link LifecycleFilter}
         */
        public void setFilter(LifecycleFilter filter) {
            this.filter = filter;
        }

        /**
         * Fluent method to set the {@link LifecycleFilter} that is used to identify objects that a
         * Lifecycle Rule applies to. A rule cannot have both {@link LifecycleFilter} and the
         * deprecated {@link #prefix}.
         *
         * @param filter {@link LifecycleFilter}
         * @return This object for method chaining.
         */
        public Rule withFilter(LifecycleFilter filter) {
            setFilter(filter);
            return this;
        }
    }

    /**
     * The transition attribute of the rule describing how this object will move between different
     * storage classes in Qcloud COS.
     */
    public static class Transition implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The time, in days, between when the object is uploaded to the bucket and when it expires.
         * Should not coexist with expirationDate within one lifecycle rule.
         */
        private int days = -1;

        /**
         * The expiration date of the object and should not coexist with expirationInDays within one
         * lifecycle rule.
         */
        private Date date;

        private String storageClass;

        /**
         * Sets the time, in days, between when an object is uploaded to the bucket and when it
         * expires.
         */
        public void setDays(int expirationInDays) {
            this.days = expirationInDays;
        }

        /**
         * Returns the time in days from an object's creation to its expiration.
         */
        public int getDays() {
            return days;
        }

        /**
         * Sets the time, in days, between when an object is uploaded to the bucket and when it
         * expires, and returns a reference to this object for method chaining.
         *
         * @see Rule#setExpirationInDays(int)
         */
        public Transition withDays(int expirationInDays) {
            this.days = expirationInDays;
            return this;
        }

        /**
         * Sets the storage class of this object.
         */
        public void setStorageClass(String storageClass) {
            this.storageClass = storageClass;
        }

        /**
         * Returns the storage class of this object.
         */
        public String getStorageClass() {
            return this.storageClass;
        }

        /**
         * Sets the storage class of this object and returns a reference to this object(Transition)
         * for method chaining.
         */
        public Transition withStorageClass(String storageClass) {
            this.storageClass = storageClass;
            return this;
        }

        /**
         * Set the expiration date of this object.
         */
        public void setDate(Date expirationDate) {
            this.date = expirationDate;
        }

        /**
         * Returns the expiration date of this object.
         */
        public Date getDate() {
            return this.date;
        }

        /**
         * Set the expiration date of this object and returns a reference to this object(Transition)
         * for method chaining.
         */
        public Transition withDate(Date expirationDate) {
            this.date = expirationDate;
            return this;
        }

    }

    /**
     * The non-current-version transition attribute of the rule, describing how non-current versions
     * of objects will move between different storage classes.
     */
    public static class NoncurrentVersionTransition implements Serializable {

        /**
         * The time, in days, between when a new version of the object is uploaded to the bucket and
         * when older version are archived.
         */
        private int days = -1;

        private String storageClass;

        /**
         * Sets the time, in days, between when a new version of the object is uploaded to the
         * bucket and when older versions are archived.
         */
        public void setDays(int expirationInDays) {
            this.days = expirationInDays;
        }

        /**
         * Returns the time in days from when a new version of the object is uploaded to the bucket
         * and when older versions are archived.
         */
        public int getDays() {
            return days;
        }

        /**
         * Sets the time in days from when a new version of the object is uploaded to the bucket and
         * when older versions are archived, and returns a reference to this object for method
         * chaining.
         */
        public NoncurrentVersionTransition withDays(int expirationInDays) {
            this.days = expirationInDays;
            return this;
        }

        /**
         * Sets the storage class of this object.
         */
        public void setStorageClass(StorageClass storageClass) {
            if (storageClass == null) {
                setStorageClass((String) null);
            } else {
                setStorageClass(storageClass.toString());
            }
        }

        /**
         * Sets the storage class of this object.
         */
        public void setStorageClass(String storageClass) {
            this.storageClass = storageClass;
        }


        /**
         * Returns the storage class of this object.
         */
        public String getStorageClassAsString() {
            return this.storageClass;
        }

        /**
         * Sets the storage class of this object and returns a reference to this object for method
         * chaining.
         */
        public NoncurrentVersionTransition withStorageClass(StorageClass storageClass) {
            setStorageClass(storageClass);
            return this;
        }

        /**
         * Sets the storage class of this object and returns a reference to this object for method
         * chaining.
         */
        public NoncurrentVersionTransition withStorageClass(String storageClass) {
            setStorageClass(storageClass);
            return this;
        }
    }

}
