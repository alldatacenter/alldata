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
import java.util.List;

import com.obs.services.internal.ObsConvertor;

/**
 * Abstract class for event notification configuration
 *
 */
public class AbstractNotification extends HeaderResponse {

    public AbstractNotification() {

    }

    /**
     * Constructor
     * 
     * @param id
     *            Event notification configuration ID
     * @param filter
     *            Filtering rules
     * @param events
     *            List of event types that need to be notified
     */
    public AbstractNotification(String id, Filter filter, List<EventTypeEnum> events) {
        this.id = id;
        this.filter = filter;
        this.events = events;
    }

    protected String id;

    protected Filter filter;

    protected List<EventTypeEnum> events;

    /**
     * List of filtering rules configured for event notification
     *
     */
    public static class Filter {

        private List<FilterRule> filterRules;

        /**
         * Filtering rules configured for event notification
         *
         */
        public static class FilterRule {

            private String name;

            private String value;

            public FilterRule() {

            }

            /**
             * Constructor
             * 
             * @param name
             *            Prefix or suffix of object names for filtering
             * @param value
             *            Object name keyword in the filtering rule
             */
            public FilterRule(String name, String value) {
                this.name = name;
                this.value = value;
            }

            /**
             * Obtain the identifier that specifies whether objects are filtered
             * by object name prefix or suffix.
             * 
             * @return Identifier specifying whether objects are filtered by
             *         object name prefix or suffix
             */
            public String getName() {
                return name;
            }

            /**
             * Set the identifier that specifies whether objects are filtered by
             * object name prefix or suffix.
             * 
             * @param name
             *            Identifier specifying whether objects are filtered by
             *            object name prefix or suffix
             */
            public void setName(String name) {
                this.name = name;
            }

            /**
             * Obtain keywords of the object name.
             * 
             * @return Keywords of the object name
             */
            public String getValue() {
                return value;
            }

            /**
             * Set keywords for the object name.
             * 
             * @param value
             *            Keywords of the object name
             */
            public void setValue(String value) {
                this.value = value;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((name == null) ? 0 : name.hashCode());
                result = prime * result + ((value == null) ? 0 : value.hashCode());
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
                FilterRule other = (FilterRule) obj;
                if (name == null) {
                    if (other.name != null) {
                        return false;
                    }
                } else if (!name.equals(other.name)) {
                    return false;
                }
                if (value == null) {
                    if (other.value != null) {
                        return false;
                    }
                } else if (!value.equals(other.value)) {
                    return false;
                }
                return true;
            }

            @Override
            public String toString() {
                return "FilterRule [name=" + name + ", value=" + value + "]";
            }

        }

        /**
         * Obtain the list of filtering rules.
         * 
         * @return Filtering rule list
         */
        public List<FilterRule> getFilterRules() {
            if (this.filterRules == null) {
                this.filterRules = new ArrayList<FilterRule>();
            }
            return filterRules;
        }

        /**
         * Set the list of filtering rules.
         * 
         * @param filterRules
         *            Filtering rule list
         */
        public void setFilterRules(List<FilterRule> filterRules) {
            this.filterRules = filterRules;
        }

        /**
         * Add a filtering rule.
         * 
         * @param name
         *            Prefix or suffix of object names for filtering
         * @param value
         *            Object name keyword in the filtering rule
         */
        public void addFilterRule(String name, String value) {
            this.getFilterRules().add(new FilterRule(name, value));
        }

        @Override
        public String toString() {
            return "Filter [fileterRules=" + filterRules + "]";
        }

    }

    /**
     * Obtain the event notification configuration ID.
     * 
     * @return Event notification configuration ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the event notification configuration ID.
     * 
     * @param id
     *            Event notification configuration ID
     */
    public void setId(String id) {
        this.id = id;
    }

    @Deprecated
    public List<String> getEvents() {
        List<String> list = new ArrayList<String>();
        for (EventTypeEnum e : this.getEventTypes()) {
            list.add(ObsConvertor.transEventTypeStatic(e));
        }
        return list;
    }

    @Deprecated
    public void setEvents(List<String> events) {
        if (events != null) {
            for (String event : events) {
                EventTypeEnum e = EventTypeEnum.getValueFromCode(event);
                if (e != null) {
                    this.getEventTypes().add(e);
                }
            }
        }
    }

    /**
     * Obtain the list of event types that need to be notified.
     * 
     * @return List of event types
     */
    public List<EventTypeEnum> getEventTypes() {
        if (this.events == null) {
            this.events = new ArrayList<EventTypeEnum>();
        }
        return events;
    }

    /**
     * Set the list of event types that need to be notified.
     * 
     * @param events
     *            List of event types
     */
    public void setEventTypes(List<EventTypeEnum> events) {
        this.events = events;
    }

    /**
     * Obtain the filtering rule group.
     * 
     * @return Filtering rules
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set the filtering rule group.
     * 
     * @param filter
     *            Filtering rules
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

}
