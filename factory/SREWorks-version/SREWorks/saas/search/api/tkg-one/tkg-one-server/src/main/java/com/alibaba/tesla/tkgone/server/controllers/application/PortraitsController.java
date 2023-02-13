package com.alibaba.tesla.tkgone.server.controllers.application;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.MysqlHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import com.alibaba.tesla.tkgone.server.services.category.AddEx;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchDocumentService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchSearchService;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.elasticsearchgrammar.GetQueryGrammar;
import com.alibaba.tesla.tkgone.server.services.tsearch.TeslasearchService;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Log4j
@RestController
@RequestMapping("/teslasearch")
public class PortraitsController extends BaseController {

    @Autowired
    TeslasearchService teslasearchService;

    @Autowired
    BaseConfigService baseConfigService;

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    AddEx addEx;

    @Autowired
    GetQueryGrammar getQueryGrammar;

    @Autowired
    ElasticSearchSearchService elasticSearchSearchService;

    @Autowired
    ElasticSearchDocumentService elasticSearchDocumentService;

    private String portraitCategory = "oldPortraits";

    @RequestMapping(value = "/get_meta/get_portraits", method = RequestMethod.GET)
    public TeslaBaseResult getPortraits() {

        JSONArray retArray =
            baseConfigService.getCategoryNameContentWithDefault(portraitCategory, "portraitList", new JSONArray());
        retArray = retArray.toJavaList(JSONObject.class).stream().peek(x -> x.put(
            "name", categoryConfigService.getCategoryTypeAlias(portraitCategory, x.getString("type"))))
            .collect(Collectors.toCollection(JSONArray::new));
        return buildSucceedResult(retArray);

    }

    @RequestMapping(value = "/query/get_snapshot_list", method = RequestMethod.GET)
    public TeslaBaseResult getSnapshotList(String type) throws Exception {

        JSONObject retJson = new JSONObject();
        JSONObject aggJson = JSONObject.parseObject(String.format("{"
            + "    \"meta\": {"
            + "        \"aggField\": \"%s\","
            + "        \"type\": \"%s\""
            + "    },"
            + "    \"sort\": {"
            + "        \"field\": \"key\","
            + "        \"reverse\": true"
            + "    }"
            + "}", Constant.PARTITION_FIELD, type));

        JSONArray retArray = elasticSearchSearchService.aggByKv(aggJson);
        retJson.put("newest_snapshot", retArray.getJSONArray(0).getString(0));
        retArray = retArray.toJavaList(JSONArray.class).stream().map(x -> {
            JSONObject tmpJson = new JSONObject();
            tmpJson.put("snapshot", x.getString(0));
            tmpJson.put("ts", x.getString(0));
            return tmpJson;
        }).collect(Collectors.toCollection(JSONArray::new));
        retJson.put("snapshots", retArray);
        return buildSucceedResult(retJson);

    }

    @RequestMapping(value = "/query/get_show_component_aliasname_chinesize", method = RequestMethod.GET)
    public TeslaBaseResult getShowComponentAliasnameChinesize(String type) {

        JSONObject retJson = new JSONObject();
        JSONArray retArray = baseConfigService.getCategoryTypeNameContentWithDefault(
            portraitCategory, type, "show_component_aliasname_chinesize", new JSONArray()).toJavaList(JSONObject.class)
            .stream().filter(x -> (Boolean)x.getOrDefault("select", true)).map(x -> {

                String field = x.getString("field");
                JSONObject tmpJson = new JSONObject();
                tmpJson.put("label", categoryConfigService.getCategoryTypeSpecFieldAlias(
                    portraitCategory, type, field));
                tmpJson.put("value", field);
                return tmpJson;

            }).collect(Collectors.toCollection(JSONArray::new));
        retJson.put(type, new JSONObject());
        retJson.getJSONObject(type).put("HighChartColumn", retArray);
        retJson.getJSONObject(type).put("HighChartPie", retArray);
        return buildSucceedResult(retJson);

    }

    @RequestMapping(value = "/query/get_type_components", method = RequestMethod.GET)
    public TeslaBaseResult getTypeComponents(String type, String snapshot) {

        JSONArray retArray = baseConfigService.getCategoryTypeNameContentWithDefault(
            portraitCategory, type, "show_component_aliasname_chinesize", new JSONArray());
        retArray = retArray.toJavaList(JSONObject.class).stream()
            .filter(x -> (Boolean)x.getOrDefault("select", true)).map(x -> {

                String field = x.getString("field");
                String fieldAlias = categoryConfigService
                    .getCategoryTypeSpecFieldAlias(portraitCategory, type, field);
                return JSONObject.parseObject(String.format("{"
                    + "    \"required\": false,"
                    + "    \"render\": {"
                    + "        \"component\": \"MultipleSelect\","
                    + "        \"props\": {,"
                    + "            \"isRemoteOption\": true,"
                    + "            \"options\": ["
                    + "            ]"
                    + "        }"
                    + "    },"
                    + "    \"key\": \"%s\","
                    + "    \"label\": \"%s\""
                    + "}", field, fieldAlias));

            }).collect(Collectors.toCollection(JSONArray::new));
        return buildSucceedResult(retArray);

    }

    @RequestMapping(value = "/query/pie_aggs_type_property", method = RequestMethod.GET)
    public TeslaBaseResult pieAggsTypeProperty(String type, String snapshot, String property,
                                               HttpServletRequest httpServletRequest) throws Exception {

        List<String> params = Collections.list(httpServletRequest.getParameterNames());
        String fieldAlias = categoryConfigService
            .getCategoryTypeSpecFieldAlias(portraitCategory, type, property);
        JSONObject retJson = JSONObject.parseObject(String.format("{"
            + "    \"component\": \"HighChartPie\","
            + "    \"props\": {"
            + "        \"chartData\": {"
            + "            \"data\": ["
            + "                {"
            + "                    \"data\": ["
            + "                    ],"
            + "                    \"name\": \"%s\""
            + "                }"
            + "            ],"
            + "            \"title\": \"%s\""
            + "        }"
            + "    }"
            + "}", fieldAlias, fieldAlias));

        JSONObject requestJson = JSONObject.parseObject(String.format("{"
            + "    \"meta\": {"
            + "        \"aggSize\": 10000,"
            + "        \"aggField\": \"%s\","
            + "        \"type\": \"%s\""
            + "    },"
            + "    \"query\": {"
            + "        \"term\": {"
            + "            \"%s\": \"%s\""
            + "        },"
            + "        \"terms\": {}"
            + "    },"
            + "    \"sort\": {"
            + "        \"field\": \"key\","
            + "        \"reverse\": true"
            + "    },"
            + "    \"style\": \"kv\""
            + "}", property, type, Constant.PARTITION_FIELD, snapshot));
        for (String param : params) {
            if (param.startsWith("tag_filter_property.")) {
                String key = param.split("\\.")[1];
                String value = httpServletRequest.getParameter(param);
                requestJson.getJSONObject("query").getJSONObject("terms").put(key,
                    Arrays.asList(value.split("\\|\\|")));
            }
        }
        retJson.getJSONObject("props").getJSONObject("chartData").getJSONArray("data").getJSONObject(0)
            .put("data", elasticSearchSearchService.aggByKv(requestJson));
        return buildSucceedResult(retJson);

    }

    @RequestMapping(value = "/query/es_nodes_by_field", method = RequestMethod.GET)
    public TeslaBaseResult esNodesByField(String snapshot, String type, int size,
                                          HttpServletRequest httpServletRequest) throws Exception {

        JSONObject retJson = new JSONObject();
        JSONArray columnDefs = baseConfigService.getCategoryTypeNameContentWithDefault(
            portraitCategory, type, "show_component_aliasname_chinesize", new JSONArray());
        columnDefs = columnDefs.toJavaList(JSONObject.class).stream().peek(x -> {
            String field = x.getString("field");
            x.put("headerName", categoryConfigService.getCategoryTypeSpecFieldAlias(
                portraitCategory, type, field));
        }).collect(Collectors.toCollection(JSONArray::new));
        retJson.put("columnDefs", columnDefs);
        JSONObject queryJson = new JSONObject();
        queryJson.put(Constant.INNER_TYPE, type);
        queryJson.put(Constant.PARTITION_FIELD, snapshot);
        queryJson.put("terms", new JSONObject());
        List<String> params = Collections.list(httpServletRequest.getParameterNames());
        for (String param : params) {
            if (param.startsWith("tag_filter_property.")) {
                String key = param.split("\\.")[1];
                String value = httpServletRequest.getParameter(param);
                queryJson.getJSONObject("terms").put(key, Arrays.asList(value.split("\\|\\|")));
            }
        }

        JSONObject queryGrammar = getQueryGrammar.get(queryJson, 0, size);
        JSONObject searchRetJson = elasticSearchSearchService.search(type, null, JSONObject.toJSONString(queryGrammar), RequestMethod.POST, true);
        Integer total = searchRetJson.getJSONObject("hits").getInteger("total");
        List<Object> rowData = searchRetJson.getJSONObject("hits").getJSONArray("hits").toJavaList(JSONObject.class).stream().map(x -> x.get("_source")).collect(Collectors.toList());

        retJson.put("length", total);
        retJson.put("rowData", rowData);
        return buildSucceedResult(retJson);
    }

    @RequestMapping(value = "/report/get_params_by_neo4j_type", method = RequestMethod.GET)
    public TeslaBaseResult getParamsByNeo4jType(String neo4j_type) {

        return buildSucceedResult(JSONObject.parseObject(
            baseConfigService.getCategoryTypeNameContent(portraitCategory, neo4j_type, "reportParams")
        ));

    }

    @RequestMapping(value = "/report/report", method = RequestMethod.POST)
    public TeslaBaseResult report(@RequestBody JSONObject jsonObject) throws InterruptedException {

        ThreadPoolExecutor threadPoolExecutor = Tools.createThreadPool(1000, "report");
        String scene = jsonObject.getString("sence");
        JSONObject properties = jsonObject.getJSONObject("properties");
        properties.put("rpted", "是");
        JSONArray records = jsonObject.getJSONArray("records");

        for (JSONObject record : JSON.parseArray(records.toJSONString(), JSONObject.class)) {

            for (JSONObject target : getReportSenceTargets(scene).toJavaList(JSONObject.class)) {

                threadPoolExecutor.execute(new ReportOnce(target, record, properties));

            }

        }
        while (!threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
            threadPoolExecutor.shutdown();
        }
        return buildSucceedResult("DONE");
    }

    class ReportOnce implements Runnable {
        JSONObject target;
        JSONObject record;
        JSONObject properties;

        ReportOnce(JSONObject target, JSONObject record, JSONObject properties) {
            this.target = target;
            this.record = record;
            this.properties = properties;
        }

        private void reportOnce(JSONObject target, JSONObject record, JSONObject properties) throws Exception {
            switch (target.getString("type")) {
                case "mysql":
                    JSONObject transfer = target.getJSONObject("transfer");
                    JSONArray recordProperties = target.getJSONArray("record_properties");
                    String primaryKey = target.getString("primary_key");
                    String primaryValue = record.getString(Constant.INNER_ID);

                    List<String> sqlKeys = new ArrayList<>();
                    List<String> sqlValues = new ArrayList<>();
                    List<String> updateStringList = new ArrayList<>();

                    sqlKeys.add(primaryKey);
                    sqlValues.add(primaryValue);
                    for (String key : recordProperties.toJavaList(String.class)) {
                        sqlKeys.add(key);
                        sqlValues.add(record.getString(key));
                        updateStringList.add(String.format("%s = '%s' ", key, record.getString(key)));
                    }
                    for (String key : transfer.keySet()) {
                        String transferValue = transfer.getString(key);
                        sqlKeys.add(transferValue);
                        sqlValues.add(properties.getString(key));
                        updateStringList.add(String.format("%s = '%s'", transferValue, properties.getString(key)));
                    }

                    MysqlHelper mysqlHelper = new MysqlHelper(
                        target.getString("host"), target.getString("port"), target.getString("db"),
                        target.getString("user"), target.getString("password"));
                    String tableName = target.getString("table");
                    String sql = String.format("INSERT INTO %s (%s) VALUES('%s') ON DUPLICATE KEY UPDATE %s",
                        tableName, String.join(",", sqlKeys), String.join("','", sqlValues),
                        String.join(",", updateStringList));
                    mysqlHelper.executeUpdate(sql);
                    break;
                case "elasticsearch":
                    JSONObject esNode = JSONObject.parseObject(JSONObject.toJSONString(record));
                    transfer = target.getJSONObject("transfer");
                    recordProperties = target.getJSONArray("record_properties");
                    for (String key : recordProperties.toJavaList(String.class)) {
                        esNode.put(key, record.getString(key));
                    }
                    for (String key : transfer.keySet()) {
                        esNode.put(transfer.getString(key), properties.getString(key));
                    }
                    String partition = categoryConfigService
                        .getCategoryIndexPartition(null, record.getString(Constant.INNER_TYPE));
                    String type = record.getString(Constant.INNER_TYPE);
                    String id = record.getString(Constant.INNER_ID) + Constant.AFTER_SEPARATOR + partition;
                    log.info(String.format("insert into elasticsearch; type: %s; id: %s; node: %s",
                        type, id, esNode));
                    elasticSearchDocumentService.index(type, id, esNode);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void run() {
            try {
                reportOnce(target, record, properties);
            } catch (Exception e) {
                log.error(
                    String.format("target: %s, record: %s, properties: %s; 报备错误: ", target, record, properties), e);
            }
        }
    }

    @SuppressWarnings("unused")
    private JSONObject getSenceParam(String scene) {
        List<ConfigDto> configDtoList = baseConfigService.getConfigDtoList(ConfigDto.builder()
            .category(portraitCategory)
            .name("reportParams")
            .build());
        for (ConfigDto configDto : configDtoList) {
            if (configDto.getName().equals(scene)) {
                return JSONObject.parseObject(configDto.getContent());
            }
        }
        return new JSONObject();
    }

    private JSONArray getReportSenceTargets(String scene) {
        return baseConfigService
            .getCategoryNameContentWithDefault(portraitCategory, "reportSceneTarget", new JSONObject())
            .getJSONObject(scene).getJSONArray("target");
    }
}
