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
**/

package com.obs.services.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.obs.services.internal.utils.ObjectUtils;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Bucket lifecycle rules
 */
public class LifecycleConfiguration extends HeaderResponse {
    private List<Rule> rules;

    /**
     * Constructor
     * 
     * @param rules
     *            List of bucket lifecycle rules
     */
    public LifecycleConfiguration(List<Rule> rules) {
        this.rules = rules;
    }

    public LifecycleConfiguration() {
    }

    /**
     * Obtain the list of bucket lifecycle rules.
     * 
     * @return List of bucket lifecycle rules
     */
    public List<Rule> getRules() {
        if (this.rules == null) {
            this.rules = new ArrayList<Rule>();
        }
        return rules;
    }

    /**
     * Add a lifecycle rule.
     * 
     * @param rule
     *            Lifecycle rule
     */
    public void addRule(Rule rule) {
        if (!getRules().contains(rule)) {
            getRules().add(rule);
        }
    }

    /**
     * Create and add a lifecycle rule.
     * 
     * @param id
     *            Rule ID
     * @param prefix
     *            Object name prefix identifying one or more objects to which
     *            the rule applies
     * @param enabled
     *            Identifier that specifies whether the rule is enabled
     * @return rule Lifecycle rule
     */
    public Rule newRule(String id, String prefix, Boolean enabled) {
        Rule rule = this.new Rule(id, prefix, enabled);
        getRules().add(rule);
        return rule;
    }

    public static void setDays(TimeEvent timeEvent, Integer days) {
        if (timeEvent != null) {
            timeEvent.days = days;
        }
    }

    public static void setDate(TimeEvent timeEvent, Date date) {
        if (timeEvent != null) {
            timeEvent.date = date;
        }
    }

    public static void setStorageClass(TimeEvent timeEvent, StorageClassEnum storageClass) {
        if (timeEvent != null) {
            timeEvent.storageClass = storageClass;
        }
    }

    public abstract class TimeEvent {
        protected Integer days;

        protected Date date;

        protected StorageClassEnum storageClass;

        public TimeEvent() {
        }

        protected TimeEvent(Integer days) {
            this.days = days;
        }

        protected TimeEvent(Date date) {
            this.date = date;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((date == null) ? 0 : date.hashCode());
            result = prime * result + ((days == null) ? 0 : days.hashCode());
            result = prime * result + ((storageClass == null) ? 0 : storageClass.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TimeEvent other = (TimeEvent) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (date == null) {
                if (other.date != null) {
                    return false;
                }
            } else if (!date.equals(other.date)) {
                return false;
            }
            if (days == null) {
                if (other.days != null) {
                    return false;
                }
            } else if (!days.equals(other.days)) {
                return false;
            }
            if (storageClass != other.storageClass) {
                return false;
            }
            return true;
        }

        private LifecycleConfiguration getOuterType() {
            return LifecycleConfiguration.this;
        }

    }

    /**
     * Expiration time of a noncurrent object version
     *
     */
    public class NoncurrentVersionExpiration extends TimeEvent {

        public NoncurrentVersionExpiration() {
        }

        /**
         * Constructor
         * 
         * @param days
         *            Expiration time of the noncurrent object version, which
         *            indicates the number of days after which the object
         *            expires since it becomes a noncurrent version
         */
        public NoncurrentVersionExpiration(Integer days) {
            this.days = days;
        }

        /**
         * Obtain the expiration time of a noncurrent object version.
         * 
         * @return Expiration time of the noncurrent object version, which
         *         indicates the number of days after which the object expires
         *         since it becomes a noncurrent version
         */
        public Integer getDays() {
            return days;
        }

        /**
         * Set the expiration time of a noncurrent object version.
         * 
         * @param days
         *            Expiration time of the noncurrent object version, which
         *            indicates the number of days after which the object
         *            expires since it becomes a noncurrent version
         */
        public void setDays(Integer days) {
            this.days = days;
        }

        @Override
        public String toString() {
            return "NoncurrentVersionExpiration [days=" + days + "]";
        }

    }

    /**
     * Expiration time of an object
     */
    public class Expiration extends TimeEvent {

        public Expiration() {
        }

        /**
         * Constructor
         * 
         * @param date
         *            A specified date in which the object will expire
         */
        public Expiration(Date date) {
            super(date);
        }

        /**
         * Constructor
         * 
         * @param days
         *            Object expiration time, specifying how many days after
         *            creation will the object expire
         */
        public Expiration(Integer days) {
            super(days);
        }

        /**
         * Obtain the expiration time of the object.
         * 
         * @return Object expiration time, specifying how many days after
         *         creation will the object expire
         */
        public Integer getDays() {
            return days;
        }

        /**
         * Set the object expiration time.
         * 
         * @param days
         *            Object expiration time, specifying how many days after
         *            creation will the object expire
         */
        public void setDays(Integer days) {
            this.days = days;
            this.date = null;
        }

        /**
         * Obtain the object expiration date.
         * 
         * @return A specified date in which the object will expire
         */
        public Date getDate() {
            return ServiceUtils.cloneDateIgnoreNull(this.date);
        }

        /**
         * Obtain the object expiration date.
         * 
         * @param date
         *            A specified date in which the object will expire
         */
        public void setDate(Date date) {
            this.date = ServiceUtils.cloneDateIgnoreNull(date);
            this.days = null;
        }

        @Override
        public String toString() {
            return "Expiration [days=" + days + ", date=" + date + "]";
        }

    }

    /**
     * Object transition policy
     *
     */
    public class Transition extends TimeEvent {

        public Transition() {
            super();
        }

        /**
         * Constructor
         * 
         * @param date
         *            Date when the object is transited
         * @param storageClass
         *            Storage class of the object after it is transited.
         *            Possible values are "WARM" and "COLD".
         */
        @Deprecated
        public Transition(Date date, String storageClass) {
            super(date);
            this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
        }

        /**
         * Constructor
         * 
         * @param date
         *            Date when the object is transited
         * @param storageClass
         *            Storage class of the object after it is transited.
         *            Possible values are "WARM" and "COLD".
         */
        public Transition(Date date, StorageClassEnum storageClass) {
            super(date);
            this.storageClass = storageClass;
        }

        /**
         * Constructor
         * 
         * @param days
         *            Object transition time, which indicates the number of days
         *            when the object is automatically transited after being
         *            created.
         * @param storageClass
         *            Storage class of the object after it is transited.
         *            Possible values are "WARM" and "COLD".
         */
        @Deprecated
        public Transition(Integer days, String storageClass) {
            super(days);
            this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
        }

        /**
         * Constructor
         * 
         * @param days
         *            Object transition time, which indicates the number of days
         *            when the object is automatically transited after being
         *            created.
         * @param storageClass
         *            Storage class of the object after it is transited.
         *            Possible values are "WARM" and "COLD".
         */
        public Transition(Integer days, StorageClassEnum storageClass) {
            super(days);
            this.storageClass = storageClass;
        }

        /**
         * Obtain the storage class of the object after transition.
         * 
         * @return Storage class of the object after transition
         * @see #getObjectStorageClass()
         */
        @Deprecated
        public String getStorageClass() {
            return storageClass != null ? this.storageClass.getCode() : null;
        }

        /**
         * Set the storage class of the object after transition.
         * 
         * @param storageClass
         *            Storage class of the object after transition
         * @see #setObjectStorageClass(StorageClassEnum storageClass)
         */
        @Deprecated
        public void setStorageClass(String storageClass) {
            this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
        }

        /**
         * Obtain the storage class of the object after transition.
         * 
         * @return Storage class of the object after transition
         */
        public StorageClassEnum getObjectStorageClass() {
            return storageClass;
        }

        /**
         * Set the storage class of the object after transition.
         * 
         * @param storageClass
         *            Storage class of the object after transition
         */
        public void setObjectStorageClass(StorageClassEnum storageClass) {
            this.storageClass = storageClass;
        }

        /**
         * Obtain the object transition time.
         * 
         * @return Object transition time, which indicates the number of days
         *         when the object is automatically transited after being
         *         created.
         */
        public Integer getDays() {
            return days;
        }

        /**
         * Set the object transition time.
         * 
         * @param days
         *            Object transition time, which indicates the number of days
         *            when the object is automatically transited after being
         *            created.
         */
        public void setDays(Integer days) {
            this.days = days;
            this.date = null;
        }

        /**
         * Obtain the object transition date.
         * 
         * @return Date when the object is transited
         */
        public Date getDate() {
            return ServiceUtils.cloneDateIgnoreNull(this.date);
        }

        /**
         * Set the object transition date.
         * 
         * @param date
         *            Date when the object is transited
         */
        public void setDate(Date date) {
            this.date = ServiceUtils.cloneDateIgnoreNull(date);
            this.days = null;
        }

        @Override
        public String toString() {
            return "Transition [days=" + days + ", date=" + date + ", storageClass=" + storageClass + "]";
        }
    }

    /**
     * Transition policy for noncurrent versions
     *
     */
    public class NoncurrentVersionTransition extends TimeEvent {
        public NoncurrentVersionTransition() {
        }

        /**
         * Constructor
         * 
         * @param days
         *            Transition time of the noncurrent object version, which
         *            indicates the number of days after which the object will
         *            be transit since it becomes a noncurrent version
         * @param storageClass
         *            Storage class of the noncurrent object version after
         *            transition
         */
        @Deprecated
        public NoncurrentVersionTransition(Integer days, String storageClass) {
            this.days = days;
            this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
        }

        /**
         * Constructor
         * 
         * @param days
         *            Transition time of the noncurrent object version, which
         *            indicates the number of days after which the object will
         *            be transit since it becomes a noncurrent version
         * @param storageClass
         *            Storage class of the noncurrent object version after
         *            transition
         */
        public NoncurrentVersionTransition(Integer days, StorageClassEnum storageClass) {
            this.days = days;
            this.storageClass = storageClass;
        }

        /**
         * Obtain the transition time of a noncurrent object version.
         * 
         * @return Transition time of the noncurrent object version, which
         *         indicates the number of days after which the object will be
         *         transit since it becomes a noncurrent version
         */
        public Integer getDays() {
            return days;
        }

        /**
         * Set the transition time of a noncurrent object version.
         * 
         * @param days
         *            Transition time of the noncurrent object version, which
         *            indicates the number of days after which the object will
         *            be transit since it becomes a noncurrent version
         */
        public void setDays(Integer days) {
            this.days = days;
        }

        /**
         * Obtain the storage class of the noncurrent object version after
         * transition.
         * 
         * @return Storage class of the noncurrent object version after
         *         transition
         * @see #getObjectStorageClass()
         */
        @Deprecated
        public String getStorageClass() {
            return storageClass != null ? this.storageClass.getCode() : null;
        }

        /**
         * Set the storage class of the noncurrent object version after
         * transition.
         * 
         * @param storageClass
         *            Storage class of the noncurrent object version after
         *            transition
         * @see #setObjectStorageClass(StorageClassEnum storageClass)
         */
        @Deprecated
        public void setStorageClass(String storageClass) {
            this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
        }

        /**
         * Obtain the storage class of the noncurrent object version after
         * transition.
         * 
         * @return Storage class of the noncurrent object version after
         *         transition
         */
        public StorageClassEnum getObjectStorageClass() {
            return storageClass;
        }

        /**
         * Set the storage class of the noncurrent object version after
         * transition.
         * 
         * @param storageClass
         *            Storage class of the noncurrent object version after
         *            transition
         */
        public void setObjectStorageClass(StorageClassEnum storageClass) {
            this.storageClass = storageClass;
        }

        @Override
        public String toString() {
            return "NoncurrentVersionTransition [days=" + days + ", storageClass=" + storageClass + "]";
        }

    }

    /**
     * Bucket lifecycle rule
     */
    public class Rule {
        protected String id;

        protected String prefix;

        protected Boolean enabled;

        protected Expiration expiration;

        protected NoncurrentVersionExpiration noncurrentVersionExpiration;

        protected List<Transition> transitions;

        protected List<NoncurrentVersionTransition> noncurrentVersionTransitions;

        /**
         * No-argument constructor
         */
        public Rule() {
        }

        /**
         * @param id
         *            Rule ID
         * @param prefix
         *            Object name prefix identifying one or more objects to
         *            which the rule applies
         * @param enabled
         *            Identifier that specifies whether the rule is enabled
         */
        public Rule(String id, String prefix, Boolean enabled) {
            this.id = id;
            this.prefix = prefix;
            this.enabled = enabled;
        }

        /**
         * Expiration time of objects
         * 
         * @return Instance of expiration time configuration
         */
        public Expiration newExpiration() {
            this.expiration = new Expiration();
            return this.expiration;
        }

        /**
         * Create the expiration time of a noncurrent object version.
         * 
         * @return Expiration time of a noncurrent object version
         */
        public NoncurrentVersionExpiration newNoncurrentVersionExpiration() {
            this.noncurrentVersionExpiration = new NoncurrentVersionExpiration();
            return this.noncurrentVersionExpiration;
        }

        /**
         * Create an object transition policy.
         * 
         * @return Object transition policy
         */
        public Transition newTransition() {
            if (this.transitions == null) {
                this.transitions = new ArrayList<Transition>();
            }
            Transition t = new Transition();
            this.transitions.add(t);
            return t;
        }

        /**
         * Create the transition policy for noncurrent versions.
         * 
         * @return Transition policy for noncurrent versions
         */
        public NoncurrentVersionTransition newNoncurrentVersionTransition() {
            if (this.noncurrentVersionTransitions == null) {
                this.noncurrentVersionTransitions = new ArrayList<NoncurrentVersionTransition>();
            }
            NoncurrentVersionTransition nt = new NoncurrentVersionTransition();
            this.noncurrentVersionTransitions.add(nt);
            return nt;
        }

        /**
         * Obtain the rule ID.
         * 
         * @return Rule ID
         */
        public String getId() {
            return id;
        }

        /**
         * Set the rule ID.
         * 
         * @param id
         *            Rule ID
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Obtain the object name prefix used to identify one or more objects to
         * which the rule applies.
         * 
         * @return Object name prefix
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Set the object name used to identify one or more objects to which the
         * rule applies.
         * 
         * @param prefix
         *            Object name prefix
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Identify whether the rule is enabled.
         * 
         * @return Identifier that specifies whether the rule is enabled
         */
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * Specify whether to enable the rule.
         * 
         * @param enabled
         *            Identifier that specifies whether the rule is enabled
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Obtain the expiration time of an object.
         * 
         * @return Expiration time of the object
         */
        public Expiration getExpiration() {
            return expiration;
        }

        /**
         * Set the expiration time of an object.
         * 
         * @param expiration
         *            Expiration time of an object
         */
        public void setExpiration(Expiration expiration) {
            this.expiration = expiration;
        }

        /**
         * Obtain the expiration time of a noncurrent object version.
         * 
         * @return Expiration time of a noncurrent object version
         */
        public NoncurrentVersionExpiration getNoncurrentVersionExpiration() {
            return noncurrentVersionExpiration;
        }

        /**
         * Set the expiration time of a noncurrent object version.
         * 
         * @param noncurrentVersionExpiration
         *            Expiration time of a noncurrent object version
         */
        public void setNoncurrentVersionExpiration(NoncurrentVersionExpiration noncurrentVersionExpiration) {
            this.noncurrentVersionExpiration = noncurrentVersionExpiration;
        }

        /**
         * Obtain the transition policy of an object.
         * 
         * @return Object transition policy
         */
        public List<Transition> getTransitions() {
            if (this.transitions == null) {
                this.transitions = new ArrayList<Transition>();
            }
            return transitions;
        }

        /**
         * Set the object transition policy.
         * 
         * @param transitions
         *            Object transition policy
         */
        public void setTransitions(List<Transition> transitions) {
            this.transitions = transitions;
        }

        /**
         * Obtain the transition policy of noncurrent versions.
         * 
         * @return Transition policy for noncurrent versions
         */
        public List<NoncurrentVersionTransition> getNoncurrentVersionTransitions() {
            if (this.noncurrentVersionTransitions == null) {
                this.noncurrentVersionTransitions = new ArrayList<NoncurrentVersionTransition>();
            }
            return noncurrentVersionTransitions;
        }

        /**
         * Set the transition policy for noncurrent versions.
         * 
         * @param noncurrentVersionTransitions
         *            Transition policy for noncurrent versions
         */
        public void setNoncurrentVersionTransitions(List<NoncurrentVersionTransition> noncurrentVersionTransitions) {
            this.noncurrentVersionTransitions = noncurrentVersionTransitions;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
            result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result
                    + ((noncurrentVersionExpiration == null) ? 0 : noncurrentVersionExpiration.hashCode());
            result = prime * result
                    + ((noncurrentVersionTransitions == null) ? 0 : noncurrentVersionTransitions.hashCode());
            result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
            result = prime * result + ((transitions == null) ? 0 : transitions.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            
            Rule other = (Rule) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(enabled, other.enabled)) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(expiration, other.expiration)) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(id, other.id)) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(noncurrentVersionExpiration, other.noncurrentVersionExpiration)) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(noncurrentVersionTransitions, other.noncurrentVersionTransitions)) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(prefix, other.prefix)) {
                return false;
            }
            
            if (!ObjectUtils.isEquals(transitions, other.transitions)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Rule [id=" + id + ", prefix=" + prefix + ", enabled=" + enabled + ", expiration=" + expiration
                    + ", noncurrentVersionExpiration=" + noncurrentVersionExpiration + ", transitions=" + transitions
                    + ", noncurrentVersionTransitions=" + noncurrentVersionTransitions + "]";
        }

        private LifecycleConfiguration getOuterType() {
            return LifecycleConfiguration.this;
        }

    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LifecycleConfiguration that = (LifecycleConfiguration) o;
        if (rules != null ? !rules.equals(that.rules) : that.rules != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return rules != null ? rules.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "LifecycleConfiguration [rules=" + rules + "]";
    }

}
