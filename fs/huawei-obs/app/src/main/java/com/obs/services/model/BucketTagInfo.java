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

/**
 * Bucket tagging configuration
 *
 */
public class BucketTagInfo extends HeaderResponse {
    private TagSet tagSet;

    public BucketTagInfo() {

    }

    /**
     * Constructor
     * 
     * @param tagSet
     *            Bucket tag set
     */
    public BucketTagInfo(TagSet tagSet) {
        this.tagSet = tagSet;
    }

    /**
     * Bucket tag set
     *
     */
    public static class TagSet {
        private List<Tag> tags;

        /**
         * Bucket tag
         *
         */
        public static class Tag {
            private String key;

            private String value;

            public Tag() {

            }

            /**
             * Constructor
             * 
             * @param key
             *            Tag key
             * @param value
             *            Tag value
             */
            public Tag(String key, String value) {
                this.key = key;
                this.value = value;
            }

            /**
             * Obtain the tag key.
             * 
             * @return Tag key
             */
            public String getKey() {
                return key;
            }

            /**
             * Set the tag key.
             * 
             * @param key
             *            Tag key
             */
            public void setKey(String key) {
                this.key = key;
            }

            /**
             * Obtain the tag value.
             * 
             * @return Tag value
             */
            public String getValue() {
                return value;
            }

            /**
             * Set the tag value.
             * 
             * @param value
             *            Tag value
             */
            public void setValue(String value) {
                this.value = value;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((key == null) ? 0 : key.hashCode());
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
                Tag other = (Tag) obj;
                if (key == null) {
                    if (other.key != null) {
                        return false;
                    }
                } else if (!key.equals(other.key)) {
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
        }

        /**
         * Obtain the tag list.
         * 
         * @return Tag list
         */
        public List<Tag> getTags() {
            if (tags == null) {
                tags = new ArrayList<Tag>();
            }
            return tags;
        }

        /**
         * Add a tag.
         * 
         * @param key
         *            Tag key
         * @param value
         *            Tag value
         * @return Newly added tag
         */
        public Tag addTag(String key, String value) {
            Tag t = new Tag(key, value);
            this.getTags().add(t);
            return t;
        }

        /**
         * Delete a tag.
         * 
         * @param key
         *            Tag key
         * @param value
         *            Tag value
         * @return Deleted tag
         */
        public Tag removeTag(String key, String value) {
            Tag t = new Tag(key, value);
            this.getTags().remove(t);
            return t;
        }

        /**
         * Delete a tag.
         * 
         * @param key
         *            Tag key
         * @return Deleted tag
         */
        public Tag removeTagByKey(String key) {
            if (null == this.tags) {
                return null;
            }
            for (Tag t : this.tags) {
                if (t.getKey().equals(key)) {
                    this.removeTag(t.getKey(), t.getValue());
                    return t;
                }
            }
            return null;
        }
    }

    /**
     * Obtain the tag set of a bucket.
     * 
     * @return Tag set
     */
    public TagSet getTagSet() {
        if (tagSet == null) {
            tagSet = new TagSet();
        }
        return tagSet;
    }

    /**
     * Configure the tag set for a bucket.
     * 
     * @param tagSet
     *            Tag set
     */
    public void setTagSet(TagSet tagSet) {
        this.tagSet = tagSet;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("[");
        if (tagSet != null) {
            int i = 0;
            for (TagSet.Tag t : tagSet.getTags()) {
                s.append("[").append("key=").append(t.getKey()).append(",").append("value=").append(t.getValue())
                        .append("]");
                if (i++ != tagSet.getTags().size() - 1) {
                    s.append(",");
                }
            }
        }
        s.append("]");
        return "BucketTagInfo [tagSet=[tags=" + s.toString() + "]";
    }
}
