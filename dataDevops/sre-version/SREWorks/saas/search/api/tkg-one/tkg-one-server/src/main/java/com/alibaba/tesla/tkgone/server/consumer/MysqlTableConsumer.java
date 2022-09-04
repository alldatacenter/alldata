package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.MysqlHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.consumer.handler.frontend.VariableParser;
import com.alibaba.tesla.tkgone.server.domain.DirectedAcyclicGraph;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import com.alibaba.tesla.tkgone.server.domain.frontend.ApiField;
import com.alibaba.tesla.tkgone.server.domain.frontend.ApiVarConfig;
import com.alibaba.tesla.tkgone.server.domain.frontend.FrontendPageVar;
import com.alibaba.tesla.tkgone.server.domain.frontend.InstantiatedApiVarConfig;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Log4j
@Service
public class MysqlTableConsumer extends AbstractConsumer implements InitializingBean {

    @Autowired
    VariableParser variableParser;

    @Override
    public void afterPropertiesSet() {
        int concurrentExecNum = consumerConfigService.getSingleConsumerMysqlTableConcurrentExecNum();
        int effectiveInterval = consumerConfigService.getMysqlTableEffectiveInterval();
        afterPropertiesSet(concurrentExecNum, effectiveInterval, ConsumerSourceType.mysqlTable);
    }

    @Override
    public int consumerDataByConsumerDto(ConsumerDto consumerDto) throws Exception {
        consumerDto = new ConsumerDto(consumerMapper.selectByPrimaryKey(consumerDto.getId()));
        String host = consumerDto.getSourceInfoJson().getString("host");
        String port = consumerDto.getSourceInfoJson().getString("port");
        String db = consumerDto.getSourceInfoJson().getString("db");
        String username = consumerDto.getSourceInfoJson().getString("username");
        String password = consumerDto.getSourceInfoJson().getString("password");
        String sql = consumerDto.getSourceInfoJson().getString("sql");
        sql = Tools.processTemplateString(sql,
                JSONObject.parseObject(JSONObject.toJSONString(consumerDto)).getInnerMap());
        boolean isPartition = consumerDto.getSourceInfoJson().getBooleanValue("isPartition");
        String name = consumerDto.getName();

        MysqlHelper mysqlHelper = new MysqlHelper(host, port, db, username, password);
        String partition = isPartition ? Tools.currentDataString() : null;
        final int[] alreadyFetchDataSize = {0};
        ConsumerDto finalConsumerDto = consumerDto;
        log.info(String.format("consume data by consumer, start consumer: %s", consumerDto.getName()));

        List<JSONObject> detailFrontendList = new ArrayList<>();
        mysqlHelper.executeQuery(sql, Constant.FETCH_DATA_SIZE, retList -> {
            // 前端站点导航源
            if (name.equals(Constant.FRONTEND_NAME)) {
                List<JSONObject> simpleFrontendList = new ArrayList<>();
                retList.forEach(ret -> {
                    JSONObject tabConfig = JSONObject.parseObject(ret.getString("tab_config"));
                    if (tabConfig != null && tabConfig.containsKey("esSearch") && !JSONObject.parseArray(tabConfig.getString("esSearch")).isEmpty()) {
                        detailFrontendList.add(ret);
                    } else {
                        simpleFrontendList.add(ret);
                    }
                });
                alreadyFetchDataSize[0] += saveToBackendStore(finalConsumerDto, simpleFrontendList,
                        finalConsumerDto.getImportConfigArray(), partition);
            } else {
                alreadyFetchDataSize[0] += saveToBackendStore(finalConsumerDto, retList,
                        finalConsumerDto.getImportConfigArray(), partition);
            }
        });

        if (CollectionUtils.isNotEmpty(detailFrontendList)) {
            List<List<JSONObject>> insDetailFrontends = detailFrontendList.parallelStream()
                    .map(this::instantiateFrontend).filter(CollectionUtils::isNotEmpty).collect(Collectors.toList());
            for (List<JSONObject> insDetailFrontendList : insDetailFrontends) {
                alreadyFetchDataSize[0] += saveToBackendStore(finalConsumerDto, insDetailFrontendList,
                        finalConsumerDto.getImportConfigArray(), partition);
            }
        }

        log.info(String.format("%s fetchData: %s", consumerDto.getName(), alreadyFetchDataSize[0]));
        return alreadyFetchDataSize[0];
    }

    private List<JSONObject> instantiateFrontend(JSONObject detailFrontend) {
        JSONObject tabConfig = JSONObject.parseObject(detailFrontend.getString("tab_config"));
        List<FrontendPageVar> pageVariables = JSONObject.parseArray(tabConfig.getString("esSearch")).toJavaList(FrontendPageVar.class);

        List<String> varIds = getTopoSort(detailFrontend.getString("id"), pageVariables);
        if (CollectionUtils.isEmpty(varIds)) {
            return null;
        }

        Map<String, FrontendPageVar> idPageVarMap = new HashMap<>();
        pageVariables.forEach(var -> idPageVarMap.put(var.getId(), var));

        List<JSONObject> vars = new ArrayList<>();
        for (String varId : varIds) {
            FrontendPageVar pageVar = idPageVarMap.get(varId);
            if (pageVar.getType().equals(Constant.FRONTEND_VAR_TYPE.api.name())) {
                JSONObject varConfig = pageVar.getConfig();
                ApiVarConfig apiVarConfig = ApiVarConfig.builder().url(varConfig.getString("url"))
                        .method(varConfig.getString("method")).contentType(varConfig.getString("contentType"))
                        .headers(varConfig.getJSONObject("headers")).params(varConfig.getJSONObject("params"))
                        .body(varConfig.getString("body"))
                        .fields(varConfig.getJSONArray("fields").toJavaList(ApiField.class)).build();
                // 无变量依赖
                if (CollectionUtils.isEmpty(pageVar.getDependencies())) {
                    List<JSONObject> curVars = variableParser.api(apiVarConfig);
                    if (CollectionUtils.isEmpty(curVars)) {
                        return null;
                    }
                    if (CollectionUtils.isEmpty(vars)) {
                        vars = curVars;
                    } else {
                        // 计算变量笛卡尔积
                        List<JSONObject> newVars = new ArrayList<>();
                        for(JSONObject var : vars) {
                            for (JSONObject curVar : curVars) {
                                JSONObject newVar = new JSONObject();
                                newVar.putAll(var);
                                newVar.putAll(curVar);
                                newVars.add(newVar);
                            }
                        }
                        vars = newVars;
                    }
                } else {
                    List<JSONObject> dependencyVars = parseDependencyVariables(vars, pageVar.getDependencies());
                    List<InstantiatedApiVarConfig> instantiatedApiVarConfigs = instantiateApiVarConfig(dependencyVars, apiVarConfig);
                    List<JSONObject> curVars = instantiatedApiVarConfigs.parallelStream().map(config -> {
                        List<JSONObject> instantiatedVars = variableParser.api(config);
                        if (CollectionUtils.isEmpty(instantiatedVars)) {
                            // 当前变量实例查询异常 直接过滤不进行下一步的实例化
                            return null;
                        } else {
                            JSONObject curVar = new JSONObject();
                            curVar.put("dependencyVarHashCode", config.getDependencyVar().hashCode());
                            curVar.put("dependencyVar", config.getDependencyVar());
                            curVar.put("instantiatedVars", instantiatedVars);
                            return curVar;
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());

                    if (CollectionUtils.isEmpty(curVars)) {
                        // 当前变量实例化后为空
                        log.warn(String.format("variable:[%s] frontend api variable instantiate failed，return empty var list", pageVar.getId()));
                        return null;
                    }

                    // 基于当前返回的变量构建
                    List<JSONObject> newVars = new ArrayList<>();
                    for (JSONObject curVar : curVars) {
                        int deVarHasHCode = curVar.getIntValue("dependencyVarHashCode");
                        JSONObject dependencyVar = curVar.getJSONObject("dependencyVar");
                        Set<String> deVarNames = dependencyVar.keySet();
                        vars.removeIf(var -> {
                            JSONObject obj = new JSONObject();
                            for(String varName : deVarNames) {
                                obj.put(varName, var.get(varName));
                            }
                            if (obj.hashCode() == deVarHasHCode && dependencyVar.equals(obj)) {
                                // 从已经解析的变量中找到依赖变量相等的项
                                List<JSONObject> instantiatedVars = curVar.getJSONArray("instantiatedVars").toJavaList(JSONObject.class);
                                List<JSONObject> deNewVars= instantiatedVars.stream().map(insVar -> {
                                    JSONObject newVar = new JSONObject();
                                    newVar.putAll(var);
                                    newVar.putAll(insVar);
                                    return newVar;
                                }).collect(Collectors.toList());
                                newVars.addAll(deNewVars);
                                return true;
                            } else {
                                return false;
                            }
                        });
                    }
                    vars = newVars;
                }
            }
        }

        List<JSONObject> frontends = new ArrayList<>();
        for (JSONObject var : vars) {
            JSONObject newDetailFrontend = new JSONObject();
            newDetailFrontend.putAll(detailFrontend);
            JSONObject newConfig = JSONObject.parseObject(newDetailFrontend.getString("config"));
            List<String> params = var.keySet().stream().map(key -> String.join("=", key, var.getString(key))).collect(Collectors.toList());
            List<String> values = var.keySet().stream().map(var::getString).collect(Collectors.toList());
            newConfig.put("url", newConfig.getString("url") + "?" + String.join("&", params));
            newConfig.put("keywords", newConfig.getString("keywords") + " " + String.join(" ", values));

            newDetailFrontend.put("id", newDetailFrontend.getString("id") + "_" + var.hashCode());
            newDetailFrontend.put("config", JSONObject.toJSONString(newConfig));

            frontends.add(newDetailFrontend);
        }

        return frontends;
    }

    /**
     * API变量配置实例化
     * 基于实例化后的依赖变量，实例化页面参数配置对象
     * @param dependencyVars
     * @param apiVarConfig
     */
    private List<InstantiatedApiVarConfig> instantiateApiVarConfig(List<JSONObject> dependencyVars, ApiVarConfig apiVarConfig) {
        List<InstantiatedApiVarConfig> configs = new ArrayList<>();
        for (JSONObject dependencyVar : dependencyVars) {
            InstantiatedApiVarConfig config = new InstantiatedApiVarConfig();
            config.setUrl(apiVarConfig.getUrl());
            config.setMethod(apiVarConfig.getMethod());
            config.setFields(apiVarConfig.getFields());
            config.setContentType(apiVarConfig.getContentType());
            StringSubstitutor sub = new StringSubstitutor(dependencyVar , "$(", ")");

            config.setUrl(sub.replace(config.getUrl()));

            JSONObject headers = apiVarConfig.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                config.setHeaders(JSONObject.parseObject(sub.replace(JSONObject.toJSONString(headers))));
            }

            JSONObject params = apiVarConfig.getParams();
            if (params != null && !params.isEmpty()) {
                config.setParams(JSONObject.parseObject(sub.replace(JSONObject.toJSONString(params))));
            }

            String body = apiVarConfig.getBody();
            if (StringUtils.isNotBlank(body)) {
                config.setBody(sub.replace(body));
            }

            config.setDependencyVar(dependencyVar);
            configs.add(config);
        }
        return configs;
    }

    /**
     * 解析依赖的变量
     * 将页面依赖的变量实例化(去重, 避免依赖变量实例化值出现重复)
     * @param vars
     * @param dependencies
     * @return
     */
    private List<JSONObject> parseDependencyVariables(List<JSONObject> vars, Set<String> dependencies) {
        List<JSONObject> dependencyVars = new ArrayList<>();

        Set<Integer> varCodes = new HashSet<>();

        for (JSONObject var : vars) {
            JSONObject dependencyVar = new JSONObject();
            for (String varName : dependencies) {
                dependencyVar.put(varName, var.get(varName));
            }

            int varCode = dependencyVar.hashCode();
            if (varCodes.contains(varCode)) {
                if (dependencyVars.stream().noneMatch(v -> v.hashCode() == varCode && v.equals(dependencyVar))) {
                    dependencyVars.add(dependencyVar);
                }
            } else {
                varCodes.add(varCode);
                dependencyVars.add(dependencyVar);
            }
        }

        return dependencyVars;
    }

    private List<String> getTopoSort(String pageId, List<FrontendPageVar> pageVariables) {
        Map<String, String> varNameIdMap = new HashMap<>();
        pageVariables.forEach(var -> {
            Set<String> names = var.getNames();
            names.forEach(name -> varNameIdMap.put(name, var.getId()));
        });

        Set<String> vars = varNameIdMap.keySet();
        DirectedAcyclicGraph<String> dag = new DirectedAcyclicGraph<>();
        pageVariables.forEach(var -> {
            Set<String> dependencies = var.getDependencies();
            // 不存在变量依赖
            if (CollectionUtils.isEmpty(dependencies)) {
                dag.addGraphNodeChain(null, var.getId());
            } else {
                if (vars.containsAll(dependencies)) {
                    dependencies.forEach(name ->dag.addGraphNodeChain(varNameIdMap.get(name), var.getId()));
                } else {
                    log.warn(String.format("frontend detail page variables config exception, page_id:%s, variable_id:%s",
                            pageId, var.getId()));
                }
            }
        });

        return dag.sort();
    }

    private JSONObject flatJsonObject(String parentKey, JSONObject jsonObject) {
        JSONObject ret = new JSONObject();
        if (jsonObject != null && !jsonObject.isEmpty()) {
            for (String key : jsonObject.keySet()) {
                Object value = JSONObject.toJSON(jsonObject.get(key));
                if (value instanceof JSONObject) {
                    ret.putAll(flatJsonObject(parentKey + "_" + key, (JSONObject)value));
                } else {
                    ret.put(parentKey + "_" + key, value);
                }
            }
        } else {
            ret.put(parentKey, jsonObject);
        }
        return ret;
    }

    private void flat(List<JSONObject> retList, JSONArray flatKeys) {
        if (!CollectionUtils.isEmpty(flatKeys)) {
            for (JSONObject jsonObject : retList) {
                for (String flatKey : flatKeys.toJavaList(String.class)) {
                    try {
                        jsonObject.putAll(flatJsonObject(flatKey, JSONObject.parseObject(jsonObject.getString(flatKey))));
                    } catch (Exception e) {
                        log.warn(String.format("flat json failed key:%s, msg:%s", flatKey, e));
                    }
                }
            }
        }
    }
}
