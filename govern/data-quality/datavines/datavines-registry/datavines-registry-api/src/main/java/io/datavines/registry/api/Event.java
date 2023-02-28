/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.registry.api;

public class Event {
    private String key;
    private String value;
    private Type type;

    public Event(String key, String value, Type type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public Event() {
    }

    public static EventBuilder builder() {
        return new EventBuilder();
    }

    public String key() {
        return this.key;
    }
    
    public String value() {
        return this.value;
    }

    public Type type() {
        return this.type;
    }

    public Event key(String key) {
        this.key = key;
        return this;
    }
    
    public Event value(String value) {
        this.value = value;
        return this;
    }

    public Event type(Type type) {
        this.type = type;
        return this;
    }

    public String toString() {
        return "Event(key=" + this.key() + ", value=" + this.value() + ", type=" + this.type() + ")";
    }

    public enum Type {
        ADD,
        UPDATE,
        REMOVE
    }

    public static class EventBuilder {
        private String key;
        private String value;
        private Type type;

        EventBuilder() {
        }

        public EventBuilder key(String key) {
            this.key = key;
            return this;
        }
        
        public EventBuilder value(String value) {
            this.value = value;
            return this;
        }

        public EventBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public Event build() {
            return new Event(key, value, type);
        }

        public String toString() {
            return "Event.EventBuilder(key=" + this.key +  ", value=" + this.value + ", type=" + this.type + ")";
        }
    }
}
