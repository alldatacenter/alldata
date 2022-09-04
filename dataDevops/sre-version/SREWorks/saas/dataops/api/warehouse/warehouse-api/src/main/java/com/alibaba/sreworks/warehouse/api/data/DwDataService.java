package com.alibaba.sreworks.warehouse.api.data;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.sreworks.warehouse.api.BasicApi;
import com.alibaba.sreworks.warehouse.common.constant.Constant;
import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.exception.ParamException;
import com.alibaba.sreworks.warehouse.common.type.ColumnType;
import com.alibaba.sreworks.warehouse.common.utils.Tools;
import com.google.common.base.Preconditions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


/**
 * 数仓数据接口
 */
public interface DwDataService extends BasicApi {

    /**
     * 模型单条数据落盘
     * @param id
     * @param data
     * @return 实际写入数据条数
     * @throws Exception
     */
    int flushDwData(Long id, JSONObject data) throws Exception;

    /**
     * 模型批量数据落盘
     * @param id
     * @param datas
     * @return 实际写入数据条数
     * @throws Exception
     */
    int flushDwDatas(Long id, List<JSONObject> datas) throws Exception;

    /**
     * 模型单条数据落盘
     * @param name
     * @param data
     * @return 实际写入数据条数
     * @throws Exception
     */
    int flushDwData(String name, JSONObject data) throws Exception;

    /**
     * 模型批量数据落盘
     * @param name
     * @param datas
     * @return 实际写入数据条数
     * @throws Exception
     */
    int flushDwDatas(String name, List<JSONObject> datas) throws Exception;

    default JSONObject convertToESData(JSONObject data, String partitionFormat, JSONObject modelFields) {
        JSONObject esData = new JSONObject();

        // generate default ds
        if (!data.containsKey(DwConstant.PARTITION_FIELD)) {
            String ds;
            switch (partitionFormat) {
                case DwConstant.PARTITION_BY_HOUR:
                    ds = DateTimeFormatter.ofPattern(DwConstant.FORMAT_HOUR).format(LocalDateTime.now());
                    break;
                case DwConstant.PARTITION_BY_DAY:
                    ds = DateTimeFormatter.ofPattern(DwConstant.FORMAT_DAY).format(LocalDateTime.now());
                    break;
                case DwConstant.PARTITION_BY_MONTH:
                    ds = DateTimeFormatter.ofPattern(DwConstant.FORMAT_MONTH).format(LocalDateTime.now());
                    break;
                case DwConstant.PARTITION_BY_YEAR:
                    ds = DateTimeFormatter.ofPattern(DwConstant.FORMAT_YEAR).format(LocalDateTime.now());
                    break;
                case DwConstant.PARTITION_BY_WEEK:
                    ds = DateTimeFormatter.ofPattern(DwConstant.FORMAT_WEEK).format(LocalDateTime.now());
                    break;
                default:
                    ds = DateTimeFormatter.ofPattern(DwConstant.FORMAT_DAY).format(LocalDateTime.now());
            }
            data.put(DwConstant.PARTITION_DIM, ds);
        }

        for(String fieldName : modelFields.keySet()) {
            JSONObject swField = modelFields.getJSONObject(fieldName);
            ColumnType fieldType = ColumnType.valueOf(swField.getString("type").toUpperCase());
            try {
                esData.put(swField.getString("dim"), parseFieldValue(data.get(fieldName), fieldName, fieldType, swField.getBoolean("nullable")));
            } catch (Exception ex) {
                throw new RuntimeException(String.format("入库前数据校验失败, 请仔细核对元信息定义, 详情:%s",ex));
            }

            // generate doc id
            if (fieldName.equals(DwConstant.PRIMARY_FIELD)) {
                esData.put(DwConstant.META_ID, esData.get(swField.getString("dim")));
            }

            if (fieldName.equals(DwConstant.PARTITION_FIELD)) {
                String ds = esData.getString(swField.getString("dim"));
                switch (partitionFormat) {
                    case DwConstant.PARTITION_BY_HOUR:
                        Preconditions.checkArgument(ds.length() == DwConstant.FORMAT_HOUR.length(), "时间分区字段非法, 合法格式:" + DwConstant.FORMAT_HOUR);
                        break;
                    case DwConstant.PARTITION_BY_DAY:
                        Preconditions.checkArgument(ds.length() == DwConstant.FORMAT_DAY.length(), "时间分区字段非法, 合法格式:" + DwConstant.FORMAT_DAY);
                        break;
                    case DwConstant.PARTITION_BY_MONTH:
                        Preconditions.checkArgument(ds.length() == DwConstant.FORMAT_MONTH.length(), "时间分区字段非法, 合法格式:" + DwConstant.FORMAT_MONTH);
                        break;
                    case DwConstant.PARTITION_BY_YEAR:
                        Preconditions.checkArgument(ds.length() == DwConstant.FORMAT_YEAR.length(), "时间分区字段非法, 合法格式:" + DwConstant.FORMAT_YEAR);
                        break;
                    case DwConstant.PARTITION_BY_WEEK:
                        Preconditions.checkArgument(ds.length() == DwConstant.FORMAT_WEEK.length(), "时间分区字段非法, 合法格式:" + DwConstant.FORMAT_WEEK);
                        break;
                    default:
                        Preconditions.checkArgument(ds.length() == DwConstant.FORMAT_DAY.length(), "时间分区字段非法, 默认按天分区, 合法格式:" + DwConstant.FORMAT_DAY);
                }

                esData.put(DwConstant.PARTITION_DIM, ds);
            }
        }

        // generate doc id
        esData.put(DwConstant.META_ID, esData.get(DwConstant.META_ID) + "_" + esData.get(DwConstant.PARTITION_DIM));

        return esData;
    }

    default Object parseFieldValue(Object value, String fieldName, ColumnType fieldType, Boolean nullable) throws Exception {
        switch (fieldType) {
            case BOOLEAN:
                value = TypeUtils.castToBoolean(value);
                break;
            case INTEGER:
                value = TypeUtils.castToInt(value);
                break;
            case LONG:
                value = TypeUtils.castToLong(value);
                break;
            case FLOAT:
                value = TypeUtils.castToFloat(value);
                break;
            case DOUBLE:
                value = TypeUtils.castToDouble(value);
                break;
            case STRING:
                value = TypeUtils.castToString(value);
                break;
            case DATE:
                if (value != null) {
                    if (value instanceof Number) {
                        Long number = ((Number)value).longValue();
                        if (number <= Constant.MAX_SECONDS_TIMESTAMP) {
                            value = TypeUtils.castToDate(number, "unixtime");
                        } else {
                            value = TypeUtils.castToDate(number);
                        }
                    } else if (value instanceof String) {
                        value = TypeUtils.castToDate(value);
                    } else {
                        throw new ParamException(String.format("请求列[%s]类型[%s]异常, 原值[%s], 解析失败", fieldName, fieldType, value));
                    }
                }
                break;
            case OBJECT: case ARRAY:
                value = value != null ? Tools.richDoc(value) : null;
                break;
            default:
                throw new ParamException(String.format("请求列[%s]类型[%s]非法, 原值[%s], 请仔细核对接口定义", fieldName, fieldType, value));
        }

        if (!nullable && value == null) {
            throw new ParamException(String.format("请求列[%s]必填, 请仔细核对接口定义", fieldName));
        }

        return value;
    }
}
