/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.sort.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.pojo.fieldformat.ArrayFormat;
import org.apache.inlong.manager.pojo.fieldformat.DecimalFormat;
import org.apache.inlong.manager.pojo.fieldformat.MapFormat;
import org.apache.inlong.manager.pojo.fieldformat.StructFormat;
import org.apache.inlong.manager.pojo.fieldformat.StructFormat.Element;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.sort.formats.common.ArrayFormatInfo;
import org.apache.inlong.sort.formats.common.BooleanFormatInfo;
import org.apache.inlong.sort.formats.common.ByteFormatInfo;
import org.apache.inlong.sort.formats.common.ByteTypeInfo;
import org.apache.inlong.sort.formats.common.DateFormatInfo;
import org.apache.inlong.sort.formats.common.DecimalFormatInfo;
import org.apache.inlong.sort.formats.common.DoubleFormatInfo;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LocalZonedTimestampFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.MapFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.ShortFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimeFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;

import java.util.List;

/**
 * Util for sort field info.
 */
@Slf4j
public class FieldInfoUtils {

    public static FieldInfo parseSinkFieldInfo(SinkField sinkField, String nodeId) {
        boolean isMetaField = sinkField.getIsMetaField() == 1;
        FieldInfo fieldInfo = getFieldInfo(sinkField.getFieldName(),
                sinkField.getFieldType(), isMetaField, sinkField.getMetaFieldName(),
                sinkField.getFieldFormat());
        fieldInfo.setNodeId(nodeId);
        return fieldInfo;
    }

    public static FieldInfo parseStreamFieldInfo(StreamField streamField, String nodeId) {
        boolean isMetaField = streamField.getIsMetaField() == 1;
        FieldInfo fieldInfo = getFieldInfo(streamField.getFieldName(), streamField.getFieldType(),
                isMetaField, streamField.getMetaFieldName(), streamField.getFieldFormat());
        fieldInfo.setNodeId(nodeId);
        return fieldInfo;
    }

    public static FieldInfo parseStreamField(StreamField streamField) {
        boolean isMetaField = streamField.getIsMetaField() == 1;
        FieldInfo fieldInfo = getFieldInfo(streamField.getFieldName(), streamField.getFieldType(),
                isMetaField, streamField.getMetaFieldName(), streamField.getFieldFormat());
        fieldInfo.setNodeId(streamField.getOriginNodeName());
        return fieldInfo;
    }

    /*
     * Get field info list.
     * TODO 1. Support partition field(not need to add index at 0), 2. Add is_metadata field in StreamSinkFieldEntity
     */
    /*public static List<FieldMappingUnit> createFieldInfo(
            List<StreamField> streamFieldList, List<SinkField> fieldList,
            List<FieldInfo> sourceFields, List<FieldInfo> sinkFields) {

        // Set source field info list.
        for (StreamField field : streamFieldList) {
            FieldInfo sourceField = getFieldInfo(field.getFieldName(), field.getFieldType(),
                    field.getIsMetaField() == 1, field.getMetaFieldName(), field.getFieldFormat());
            sourceFields.add(sourceField);
        }

        List<FieldMappingUnit> mappingUnitList = new ArrayList<>();
        // Get sink field info list, if the field name equals to build-in field, new a build-in field info
        for (SinkField field : fieldList) {
            FieldInfo sinkField = getFieldInfo(field.getFieldName(), field.getFieldType(),
                    field.getIsMetaField() == 1, field.getMetaFieldName(), field.getFieldFormat());
            sinkFields.add(sinkField);
            if (StringUtils.isNotBlank(field.getSourceFieldName())) {
                FieldInfo sourceField = getFieldInfo(field.getSourceFieldName(),
                        field.getSourceFieldType(), field.getIsMetaField() == 1,
                        field.getMetaFieldName(), field.getFieldFormat());
                mappingUnitList.add(new FieldMappingUnit(sourceField, sinkField));
            }
        }

        return mappingUnitList;
    }*/

    /**
     * Get field info by the given field name ant type.
     *
     * @apiNote If the field name equals to build-in field, new a build-in field info
     */
    private static FieldInfo getFieldInfo(String fieldName, String fieldType,
            boolean isMetaField, String metaFieldName, String format) {
        if (isMetaField) {
            // TODO The meta field needs to be selectable and cannot be filled in by the user
            return new MetaFieldInfo(fieldName, MetaField.forName(metaFieldName));
        } else {
            return new FieldInfo(fieldName, convertFieldFormat(fieldType, format));
        }
    }

    /*
     * Get all migration field mapping unit list for binlog source.
     */
    /*public static List<FieldMappingUnit> setAllMigrationFieldMapping(List<FieldInfo> sourceFields,
            List<FieldInfo> sinkFields) {
        List<FieldMappingUnit> mappingUnitList = new ArrayList<>();
        MetaFieldInfo dataField = new MetaFieldInfo("data", MetaField.DATA);
        sourceFields.add(dataField);
        sinkFields.add(dataField);
        mappingUnitList.add(new FieldMappingUnit(dataField, dataField));
        // TODO discarded later
        for (MetaField metaField : MetaField.values()) {
            MetaFieldInfo fieldInfo = new MetaFieldInfo(metaField.name(), metaField);
            sourceFields.add(fieldInfo);
            sinkFields.add(fieldInfo);
            mappingUnitList.add(new FieldMappingUnit(fieldInfo, fieldInfo));
        }

        return mappingUnitList;
    }*/

    /**
     * Get the FieldFormat of Sort according to type string and format of field
     *
     * @param type type string
     * @return Sort field format instance
     */
    public static FormatInfo convertFieldFormat(String type) {
        return convertFieldFormat(type, null);
    }

    /**
     * Get the FieldFormat of Sort according to type string
     *
     * @param type type string
     * @return Sort field format instance
     */
    public static FormatInfo convertFieldFormat(String type, String format) {
        FormatInfo formatInfo;
        FieldType fieldType = FieldType.forName(type);
        switch (fieldType) {
            case BOOLEAN:
                formatInfo = new BooleanFormatInfo();
                break;
            case INT8:
            case TINYINT:
            case BYTE:
                formatInfo = new ByteFormatInfo();
                break;
            case INT16:
            case SMALLINT:
            case SHORT:
                formatInfo = new ShortFormatInfo();
                break;
            case INT32:
            case INT:
                formatInfo = new IntFormatInfo();
                break;
            case INT64:
            case BIGINT:
            case LONG:
                formatInfo = new LongFormatInfo();
                break;
            case FLOAT32:
            case FLOAT:
                formatInfo = new FloatFormatInfo();
                break;
            case FLOAT64:
            case DOUBLE:
                formatInfo = new DoubleFormatInfo();
                break;
            case DECIMAL:
                if (StringUtils.isNotBlank(format)) {
                    DecimalFormat decimalFormat = FieldFormatUtils.parseDecimalFormat(format);
                    formatInfo = new DecimalFormatInfo(decimalFormat.getPrecision(), decimalFormat.getScale());
                } else {
                    formatInfo = new DecimalFormatInfo();
                }
                break;
            case DATE:
                if (StringUtils.isNotBlank(format)) {
                    formatInfo = new DateFormatInfo(convertTimestampOrDataFormat(format));
                } else {
                    formatInfo = new DateFormatInfo();
                }
                break;
            case DATETIME:
            case TIME:
                if (StringUtils.isNotBlank(format)) {
                    formatInfo = new TimeFormatInfo(convertTimestampOrDataFormat(format));
                } else {
                    formatInfo = new TimeFormatInfo();
                }
                break;
            case TIMESTAMP:
                if (StringUtils.isNotBlank(format)) {
                    formatInfo = new TimestampFormatInfo(convertTimestampOrDataFormat(format));
                } else {
                    formatInfo = new TimestampFormatInfo();
                }
                break;
            case LOCAL_ZONE_TIMESTAMP:
                if (StringUtils.isNotBlank(format)) {
                    formatInfo = new LocalZonedTimestampFormatInfo(convertTimestampOrDataFormat(format), 2);
                } else {
                    formatInfo = new LocalZonedTimestampFormatInfo();
                }
                break;
            case BINARY:
            case FIXED:
                formatInfo = new ArrayFormatInfo(ByteTypeInfo::new);
                break;
            case ARRAY:
                formatInfo = createArrayFormatInfo(format);
                break;
            case MAP:
                formatInfo = createMapFormatInfo(format);
                break;
            case STRUCT:
                formatInfo = createRowFormatInfo(format);
                break;
            default: // default is string
                formatInfo = new StringFormatInfo();
        }
        return formatInfo;
    }

    private static ArrayFormatInfo createArrayFormatInfo(String format) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("Unsupported array type without format");
        }
        ArrayFormat arrayFormat = FieldFormatUtils.parseArrayFormat(format);
        FormatInfo elementFormatInfo = convertFieldFormat(arrayFormat.getElementType().name(),
                arrayFormat.getElementFormat());
        return new ArrayFormatInfo(elementFormatInfo);
    }

    private static MapFormatInfo createMapFormatInfo(String format) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("Unsupported map type without format");
        }
        MapFormat mapFormat = FieldFormatUtils.parseMapFormat(format);
        FormatInfo keyFormatInfo = convertFieldFormat(mapFormat.getKeyType().name(), mapFormat.getKeyFormat());
        FormatInfo valueFormatInfo = convertFieldFormat(mapFormat.getValueType().name(), mapFormat.getValueFormat());
        return new MapFormatInfo(keyFormatInfo, valueFormatInfo);
    }

    private static RowFormatInfo createRowFormatInfo(String format) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("Unsupported struct type without format");
        }
        StructFormat structFormat = FieldFormatUtils.parseStructFormat(format);
        List<String> fieldNames = Lists.newArrayList();
        List<FormatInfo> formatInfos = Lists.newArrayList();
        for (Element element : structFormat.getElements()) {
            fieldNames.add(element.getFieldName());
            formatInfos.add(convertFieldFormat(element.getFieldType().name(), element.getFieldFormat()));
        }
        return new RowFormatInfo(fieldNames.toArray(new String[0]), formatInfos.toArray(new FormatInfo[0]));
    }

    /**
     * Convert to sort field format
     *
     * @param format The format
     * @return The sort format
     */
    private static String convertTimestampOrDataFormat(String format) {
        String sortFormat = format;
        switch (format) {
            case "MICROSECONDS":
                sortFormat = "MICROS";
                break;
            case "MILLISECONDS":
                sortFormat = "MILLIS";
                break;
            case "SECONDS":
                sortFormat = "SECONDS";
                break;
            default:
        }
        return sortFormat;
    }

}
