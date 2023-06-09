package com.alibaba.datax.plugin.writer.elasticsearchwriter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.aliases.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by xiongfeng.bxf on 17/2/8.
 */
public class ESClient {
    private static final Logger log = LoggerFactory.getLogger(ESClient.class);
    final ESInitialization es;

    public ESClient(ESInitialization es) {
        this.es = es;
    }


    public boolean indicesExists(String indexName) throws Exception {
        boolean isIndicesExists = false;
        JestResult rst = es.jestClient.execute(new IndicesExists.Builder(indexName).build());
        if (rst.isSucceeded()) {
            isIndicesExists = true;
        } else {
            switch (rst.getResponseCode()) {
                case 404:
                    isIndicesExists = false;
                    break;
                case 401:
                    // 无权访问
                default:
                    log.warn(rst.getErrorMessage());
                    break;
            }
        }
        return isIndicesExists;
    }

    public boolean deleteIndex(String indexName) throws Exception {
        log.info("delete index " + indexName);
        if (indicesExists(indexName)) {
            JestResult rst = execute(new DeleteIndex.Builder(indexName).build());
            if (!rst.isSucceeded()) {
                return false;
            }
        } else {
            log.info("index cannot found, skip delete " + indexName);
        }
        return true;
    }

    public boolean createIndex(String indexName, String typeName,
                               Object mappings, String settings, boolean dynamic) throws Exception {
        JestResult rst = null;
        if (!indicesExists(indexName)) {
            log.info("create index " + indexName);
            rst = es.jestClient.execute(
                    new CreateIndex.Builder(indexName)
                            .settings(settings)
                            .mappings(String.valueOf(mappings))
                            .setParameter("master_timeout", "5m")
                            .build()
            );
            //index_already_exists_exception
            if (!rst.isSucceeded()) {
                log.error("createIndex faild:{} ", rst.getErrorMessage());
                //if (getStatus(rst) == 400) {
                //   log.info(String.format("index [%s] already exists", indexName));
                // return true;
                //}
//                else {
//                    log.error(rst.getErrorMessage());
//
//                }
                return false;
            } else {
                log.info(String.format("create [%s] index success", indexName));
            }
        }
//        int idx = 0;
//        while (idx < 5) {
//            if (indicesExists(indexName)) {
//                break;
//            }
//            Thread.sleep(2000);
//            idx++;
//        }
//        if (idx >= 5) {
//            return false;
//        }
//
//        if (dynamic) {
//            log.info("ignore mappings");
//            return true;
//        }
//        log.info("create mappings for " + indexName + "  " + mappings);
//        rst = jestClient.execute(new PutMapping.Builder(indexName, typeName, mappings)
//                .setParameter("master_timeout", "5m").build());
//        if (!rst.isSucceeded()) {
//            if (getStatus(rst) == 400) {
//                log.info(String.format("index [%s] mappings already exists", indexName));
//            } else {
//                log.error(rst.getErrorMessage());
//                return false;
//            }
//        } else {
//            log.info(String.format("index [%s] put mappings success", indexName));
//        }
        return true;
    }

    public JestResult execute(Action<JestResult> clientRequest) throws Exception {
        JestResult rst = null;
        rst = es.jestClient.execute(clientRequest);
        if (!rst.isSucceeded()) {
            //log.warn(rst.getErrorMessage());
        }
        return rst;
    }

    public Integer getStatus(JestResult rst) {
        JsonObject jsonObject = rst.getJsonObject();
        if (jsonObject.has("status")) {
            return jsonObject.get("status").getAsInt();
        }
        return 600;
    }

    public boolean isBulkResult(JestResult rst) {
        JsonObject jsonObject = rst.getJsonObject();
        return jsonObject.has("items");
    }


    public boolean alias(String indexname, String aliasname, boolean needClean) throws IOException {
        GetAliases getAliases = new GetAliases.Builder().addIndex(aliasname).build();
        AliasMapping addAliasMapping = new AddAliasMapping.Builder(indexname, aliasname).build();
        JestResult rst = es.jestClient.execute(getAliases);
        log.info(rst.getJsonString());
        List<AliasMapping> list = new ArrayList<AliasMapping>();
        if (rst.isSucceeded()) {
            JsonParser jp = new JsonParser();
//            JSONObject jo = JSONObject.parseObject(rst.getJsonString());
            JsonObject jo = (JsonObject) jp.parse(rst.getJsonString());
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                String tindex = entry.getKey();
                if (indexname.equals(tindex)) {
                    continue;
                }
                AliasMapping m = new RemoveAliasMapping.Builder(tindex, aliasname).build();
                String s = new Gson().toJson(m.getData());
                log.info(s);
                if (needClean) {
                    list.add(m);
                }
            }
        }

        ModifyAliases modifyAliases = new ModifyAliases.Builder(addAliasMapping).addAlias(list).setParameter("master_timeout", "5m").build();
        rst = es.jestClient.execute(modifyAliases);
        if (!rst.isSucceeded()) {
            log.error(rst.getErrorMessage());
            return false;
        }
        return true;
    }

    public JestResult bulkInsert(Bulk.Builder bulk, int trySize) throws Exception {
        // es_rejected_execution_exception
        // illegal_argument_exception
        // cluster_block_exception
        JestResult rst = null;
        rst = es.jestClient.execute(bulk.build());
        if (!rst.isSucceeded()) {
            log.warn(rst.getErrorMessage());
        }
        return rst;
    }

    /**
     * 关闭JestClient客户端
     */
    public void closeJestClient() {
        if (es.jestClient != null) {
            es.jestClient.shutdownClient();
        }
    }

    /**
     * https://www.elastic.co/guide/en/elasticsearch/reference/current/explicit-mapping.html
     *
     * @param typeName
     * @return
     */
    public String genMappings(JSONArray column, String typeName, Consumer<List<ESColumn>> colsConsumer) {
        String mappings = null;
        Map<String, Object> propMap = new HashMap<String, Object>();
        List<ESColumn> columnList = new ArrayList<ESColumn>();

        //   JSONArray column = (JSONArray) conf.getList("column");
        if (column != null) {
            for (Object col : column) {
                JSONObject jo = (JSONObject) col;
                String colName = jo.getString("name");
                String colTypeStr = jo.getString("type");
                if (colTypeStr == null) {
                    //throw DataXException.asDataXException(ESWriterErrorCode.BAD_CONFIG_VALUE, col.toString() + " column must have type");
                    throw new IllegalStateException(col.toString() + " column must have type");
                }
                ESFieldType colType = ESFieldType.getESFieldType(colTypeStr);
                if (colType == null) {
                    // throw DataXException.asDataXException(ESWriterErrorCode.BAD_CONFIG_VALUE, col.toString() + " unsupported type");
                    throw new IllegalStateException(col.toString() + " unsupported type");
                }

                ESColumn columnItem = new ESColumn();

                if (colName.equals(Key.PRIMARY_KEY_COLUMN_NAME)) {
                    // 兼容已有版本
                    colType = ESFieldType.ID;
                    colTypeStr = "id";
                }

                columnItem.setName(colName);
                columnItem.setType(colTypeStr);

                if (colType == ESFieldType.ID) {
                    columnList.add(columnItem);
                    // 如果是id,则properties为空
                    continue;
                }

                Boolean array = jo.getBoolean("array");
                if (array != null) {
                    columnItem.setArray(array);
                }
                Map<String, Object> field = new HashMap<String, Object>();
                field.put("type", colTypeStr);
                //https://www.elastic.co/guide/en/elasticsearch/reference/5.2/breaking_50_mapping_changes.html#_literal_index_literal_property
                // https://www.elastic.co/guide/en/elasticsearch/guide/2.x/_deep_dive_on_doc_values.html#_disabling_doc_values
                field.put("doc_values", jo.getBoolean("doc_values"));
                field.put("ignore_above", jo.getInteger("ignore_above"));
                field.put("index", jo.getBoolean("index"));

                switch (colType) {
//                    case STRING:
//                        // 兼容string类型,ES5之前版本
//                        break;
                    case KEYWORD:
                        // https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-search-speed.html#_warm_up_global_ordinals
                        Boolean eagerGlobalOrdinals = jo.getBoolean("eager_global_ordinals");
                        if (eagerGlobalOrdinals != null) {
                            field.put("eager_global_ordinals", eagerGlobalOrdinals);
                        }
                        break;
                    case TEXT:
                        field.put("analyzer", jo.getString("analyzer"));
                        // 优化disk使用,也同步会提高index性能
                        // https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-disk-usage.html
                        field.put("norms", jo.getBoolean("norms"));
                        field.put("index_options", jo.getBoolean("index_options"));
                        break;
                    case DATE:
                        columnItem.setTimeZone(jo.getString("timezone"));
                        columnItem.setFormat(jo.getString("format"));
                        // 后面时间会处理为带时区的标准时间,所以不需要给ES指定格式
                            /*
                            if (jo.getString("format") != null) {
                                field.put("format", jo.getString("format"));
                            } else {
                                //field.put("format", "strict_date_optional_time||epoch_millis||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd");
                            }
                            */
                        break;
                    case GEO_SHAPE:
                        field.put("tree", jo.getString("tree"));
                        field.put("precision", jo.getString("precision"));
                    default:
                        break;
                }
                propMap.put(colName, field);
                columnList.add(columnItem);
            }
        } else {
            throw new IllegalStateException("conf.getList(\"column\") can not be empty");
        }

        colsConsumer.accept(columnList);
        // conf.set(WRITE_COLUMNS, JSON.toJSONString(columnList));


        Map<String, Object> rootMappings = new HashMap<String, Object>();
        Map<String, Object> typeMappings = new HashMap<String, Object>();
        typeMappings.put("properties", propMap);
        rootMappings.put(typeName, typeMappings);

        mappings = StringUtils.isNotBlank(typeName) ? JSON.toJSONString(rootMappings) : JSON.toJSONString(typeMappings);

        if (StringUtils.isEmpty(mappings)) {
            //throw DataXException.asDataXException(ESWriterErrorCode.BAD_CONFIG_VALUE, "must have mappings");
            throw new IllegalStateException("must have mappings");
        }
        log.info(mappings);
        return mappings;
    }

}
