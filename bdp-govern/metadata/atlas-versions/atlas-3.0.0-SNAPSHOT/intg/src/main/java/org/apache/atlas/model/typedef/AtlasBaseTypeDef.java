/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.typedef;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.TypeCategory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * Base class that captures common-attributes for all Atlas types.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class AtlasBaseTypeDef implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ATLAS_TYPE_BOOLEAN        = "boolean";
    public static final String ATLAS_TYPE_BYTE           = "byte";
    public static final String ATLAS_TYPE_SHORT          = "short";
    public static final String ATLAS_TYPE_INT            = "int";
    public static final String ATLAS_TYPE_LONG           = "long";
    public static final String ATLAS_TYPE_FLOAT          = "float";
    public static final String ATLAS_TYPE_DOUBLE         = "double";
    public static final String ATLAS_TYPE_BIGINTEGER     = "biginteger";
    public static final String ATLAS_TYPE_BIGDECIMAL     = "bigdecimal";
    public static final String ATLAS_TYPE_STRING         = "string";
    public static final String ATLAS_TYPE_DATE           = "date";
    public static final String ATLAS_TYPE_OBJECT_ID      = "objectid";

    public static final String ATLAS_TYPE_ARRAY_PREFIX    = "array<";
    public static final String ATLAS_TYPE_ARRAY_SUFFIX    = ">";
    public static final String ATLAS_TYPE_MAP_PREFIX      = "map<";
    public static final String ATLAS_TYPE_MAP_KEY_VAL_SEP = ",";
    public static final String ATLAS_TYPE_MAP_SUFFIX      = ">";

    public static final String ATLAS_TYPE_PROCESS        = "Process";
    public static final String ATLAS_TYPE_DATASET        = "DataSet";
    public static final String ATLAS_TYPE_ASSET          = "Asset";
    public static final String ATLAS_TYPE_INFRASTRUCTURE = "Infrastructure";

    public static final String TYPEDEF_OPTION_SUPPORTS_SCHEMA  = "supportsSchema";
    public static final String TYPEDEF_OPTION_SUPPORTS_PROFILE = "supportsProfile";

    public static final String[] ATLAS_PRIMITIVE_TYPES = {
        ATLAS_TYPE_BOOLEAN,
        ATLAS_TYPE_BYTE,
        ATLAS_TYPE_SHORT,
        ATLAS_TYPE_INT,
        ATLAS_TYPE_LONG,
        ATLAS_TYPE_FLOAT,
        ATLAS_TYPE_DOUBLE,
        ATLAS_TYPE_BIGINTEGER,
        ATLAS_TYPE_BIGDECIMAL,
        ATLAS_TYPE_STRING,
    };
    /**
     * The list of types that are valid for relationships. These are the
     * primitive attributes and date.
     */
    public static final String[] ATLAS_RELATIONSHIP_ATTRIBUTE_TYPES = { ATLAS_TYPE_BOOLEAN,
            ATLAS_TYPE_BYTE,
            ATLAS_TYPE_SHORT,
            ATLAS_TYPE_INT,
            ATLAS_TYPE_LONG,
            ATLAS_TYPE_FLOAT,
            ATLAS_TYPE_DOUBLE,
            ATLAS_TYPE_BIGINTEGER,
            ATLAS_TYPE_BIGDECIMAL,
            ATLAS_TYPE_STRING,
            ATLAS_TYPE_DATE
    };

    public static final String[] ATLAS_BUILTIN_TYPES = {
        ATLAS_TYPE_BOOLEAN,
        ATLAS_TYPE_BYTE,
        ATLAS_TYPE_SHORT,
        ATLAS_TYPE_INT,
        ATLAS_TYPE_LONG,
        ATLAS_TYPE_FLOAT,
        ATLAS_TYPE_DOUBLE,
        ATLAS_TYPE_BIGINTEGER,
        ATLAS_TYPE_BIGDECIMAL,
        ATLAS_TYPE_STRING,

        ATLAS_TYPE_DATE,
        ATLAS_TYPE_OBJECT_ID,
    };

    public static final String     SERVICE_TYPE_ATLAS_CORE    = "atlas_core";
    public static final String     SERIALIZED_DATE_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Deprecated
    public static final DateFormat DATE_FORMATTER             = new SimpleDateFormat(SERIALIZED_DATE_FORMAT_STR);

    static {
        DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static DateFormat getDateFormatter() {
        return THREAD_LOCAL_DATE_FORMAT.get();
    }

    private static final ThreadLocal<DateFormat> THREAD_LOCAL_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        public DateFormat initialValue() {
            DateFormat ret = new SimpleDateFormat(SERIALIZED_DATE_FORMAT_STR);

            ret.setTimeZone(TimeZone.getTimeZone("UTC"));

            return ret;
        }
    };

    private final TypeCategory category;
    private String  guid       = null;
    private String  createdBy  = null;
    private String  updatedBy  = null;
    private Date    createTime = null;
    private Date    updateTime = null;
    private Long    version    = null;
    private String  name;
    private String  description;
    private String  typeVersion;
    private String  serviceType;
    private Map<String, String> options;

    protected AtlasBaseTypeDef(TypeCategory category, String name, String description, String typeVersion,
                               String serviceType, Map<String, String> options) {
        super();

        this.category = category;

        setGuid(null);
        setCreatedBy(null);
        setUpdatedBy(null);
        setCreateTime(null);
        setUpdateTime(null);
        setVersion(null);
        setName(name);
        setDescription(description);
        setTypeVersion(typeVersion);
        setServiceType(serviceType);
        setOptions(options);
    }

    protected AtlasBaseTypeDef(AtlasBaseTypeDef other) {
        if (other != null) {
            this.category = other.category;

            setGuid(other.getGuid());
            setCreatedBy(other.getCreatedBy());
            setUpdatedBy(other.getUpdatedBy());
            setCreateTime(other.getCreateTime());
            setUpdateTime(other.getUpdateTime());
            setVersion(other.getVersion());
            setName(other.getName());
            setServiceType(other.getServiceType());
            setDescription(other.getDescription());
            setTypeVersion(other.getTypeVersion());
            setOptions(other.getOptions());
        } else {
            this.category = TypeCategory.PRIMITIVE;

            setGuid(null);
            setCreatedBy(null);
            setUpdatedBy(null);
            setCreateTime(null);
            setUpdateTime(null);
            setVersion(null);
            setName(null);
            setServiceType(null);
            setDescription(null);
            setTypeVersion(null);
            setOptions(null);
        }
    }

    public TypeCategory getCategory() { return category; }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeVersion() {
        return typeVersion;
    }

    public void setTypeVersion(String typeVersion) {
        this.typeVersion = typeVersion;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        if (options != null) {
            this.options = new HashMap<>(options);
        } else {
            this.options = null;
        }
    }

    @JsonIgnore
    public String getOption(String optionName) {
        Map<String, String> options = this.options;

        return options != null ? options.get(optionName) : null;
    }

    @JsonIgnore
    public void setOption(String optionName, String value) {
        Map<String, String> options = this.options;

        if (options == null) {
            this.options = options = new HashMap<>();
        }

        options.put(optionName, value);
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasBaseTypeDef{");
        sb.append("category='").append(category).append('\'');
        sb.append(", guid='").append(guid).append('\'');
        sb.append(", createdBy='").append(createdBy).append('\'');
        sb.append(", updatedBy='").append(updatedBy).append('\'');
        dumpDateField(", createTime=", createTime, sb);
        dumpDateField(", updateTime=", updateTime, sb);
        sb.append(", version=").append(version);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", typeVersion='").append(typeVersion).append('\'');
        sb.append(", serviceType='").append(serviceType).append('\'');
        sb.append(", options='").append(options).append('\'');
        sb.append('}');

        return sb;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtlasBaseTypeDef that = (AtlasBaseTypeDef) o;

        return category == that.category &&
                Objects.equals(guid, that.guid) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(updatedBy, that.updatedBy) &&
                Objects.equals(createTime, that.createTime) &&
                Objects.equals(updateTime, that.updateTime) &&
                Objects.equals(version, that.version) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(typeVersion, that.typeVersion) &&
                Objects.equals(serviceType, that.serviceType) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        int result = category != null ? category.hashCode() : 0;
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = 31 * result + (updatedBy != null ? updatedBy.hashCode() : 0);
        result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
        result = 31 * result + (updateTime != null ? updateTime.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (typeVersion != null ? typeVersion.hashCode() : 0);
        result = 31 * result + (serviceType != null ? serviceType.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }

    public static String getArrayTypeName(String elemTypeName) {
        return  ATLAS_TYPE_ARRAY_PREFIX + elemTypeName + ATLAS_TYPE_ARRAY_SUFFIX;
    }

    public static String getMapTypeName(String keyTypeName, String valueTypeName) {
        return String.format("%s%s%s%s%s", ATLAS_TYPE_MAP_PREFIX, keyTypeName, ATLAS_TYPE_MAP_KEY_VAL_SEP,
                valueTypeName, ATLAS_TYPE_MAP_SUFFIX);
    }

    public static StringBuilder dumpObjects(Collection<?> objects, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (CollectionUtils.isNotEmpty(objects)) {
            int i = 0;
            for (Object obj : objects) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(obj);
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpObjects(Map<?, ?> objects, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (MapUtils.isNotEmpty(objects)) {
            int i = 0;
            for (Map.Entry<?, ?> e : objects.entrySet()) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(e.getKey()).append(":").append(e.getValue());
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpDateField(String prefix, Date value, StringBuilder sb) {
        sb.append(prefix);

        if (value == null) {
            sb.append(value);
        } else {
            sb.append(getDateFormatter().format(value));
        }

        return sb;
    }
}
