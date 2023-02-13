package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.*;
import com.alibaba.tesla.tkgone.server.domain.*;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import com.alibaba.tesla.tkgone.server.services.config.BackendStoreConfigService;
import com.alibaba.tesla.tkgone.server.services.config.ConsumerConfigService;
import com.alibaba.tesla.tkgone.server.services.tsearch.BackendStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yangjinghua
 */
@Slf4j
@Service
public class BasicConsumer {
    private static final int CONSUME_KEY_EXPIRE_TIME = 60 * 60 * 24;

    @Autowired
    public ConsumerMapper consumerMapper;

    @Autowired
    public ConsumerNodeMapper consumerNodeMapper;

    @Autowired
    public ConsumerHistoryMapper consumerHistoryMapper;

    @Autowired
    public ConsumerConfigService consumerConfigService;

    @Autowired
    public BackendStoreService backendStoreService;

    @Autowired
    BackendStoreConfigService backendStoreConfigService;

    @Autowired
    public RedisHelper redisHelper;

    private String localIp = Tools.getLocalIp();

    Long consumeStime; // consume开始时间

    private Boolean checkLocalRun(ConsumerDto consumerDto, String tmpIp) {
        String lockClient = consumerDto.getSourceInfoJson().getString("lockClient");
        lockClient = StringUtils.isEmpty(lockClient) ? "" : lockClient;
        if (lockClient.equals(localIp)) {
            return true;
        } else {
            return StringUtils.isEmpty(lockClient) && Objects.equals(tmpIp, localIp);
        }
    }

    List<ConsumerDto> getLocalConsumerDtoList(ConsumerSourceType sourceType) {
        List<ConsumerDto> retList = new ArrayList<>();

        int heartBeatInvalidTimeInterval = consumerConfigService.getConsumerNodeHeartBeatInvalidTimeInterval();

        ConsumerExample consumerExample = new ConsumerExample();
        consumerExample.createCriteria().andSourceTypeEqualTo(sourceType.toString())
                .andEnableEqualTo(Boolean.toString(true));
        List<ConsumerDto> allValidConsumerDtoList = consumerMapper.selectByExample(consumerExample).stream()
                .map(ConsumerDto::new).collect(Collectors.toList());
        ConsumerNodeExample consumerNodeExample = new ConsumerNodeExample();
        consumerNodeExample.createCriteria()
                .andGmtModifiedGreaterThanOrEqualTo(
                        new Date(System.currentTimeMillis() - heartBeatInvalidTimeInterval * 1000))
                .andEnableEqualTo("true");
        Set<String> clientIps = consumerNodeMapper.selectByExample(consumerNodeExample).stream()
                .map(ConsumerNode::getHost).collect(Collectors.toSet());

        String nodes = consumerConfigService.getConsumerNodes();
        if (nodes != null && !nodes.isEmpty()) {
            clientIps.retainAll(Arrays.asList(nodes.split(",")));
        }

        ConsistentHash<String> consistentHash = new ConsistentHash<>(clientIps);

        if (CollectionUtils.isEmpty(allValidConsumerDtoList)) {
            return retList;
        }
        for (ConsumerDto consumerDto : allValidConsumerDtoList) {
            String tmpIp = consistentHash.get(consumerDto.getId());
            boolean result = checkLocalRun(consumerDto, tmpIp);
            if (result) {
                retList.add(consumerDto);
                consumerDto.setClient(localIp);
                consumerMapper.updateByPrimaryKeySelective(consumerDto.toConsumer());
            }
        }
        return retList;

    }

    private List<List<JSONObject>> lineToBackendData(JSONObject line, JSONArray template) {
        List<List<JSONObject>> backendData = new ArrayList<>();
        for (JSONArray jsonArray : template.toJavaList(JSONArray.class)) {
            List<JSONObject> jsonObjectList = new ArrayList<>();
            Boolean exit = true;
            for (JSONObject jsonObject : jsonArray.toJavaList(JSONObject.class)) {
                JSONObject newJsonObject = new JSONObject();
                for (String key : jsonObject.keySet()) {
                    String value = jsonObject.getString(key);
                    try {
                        value = Tools.processTemplate(value, line);
                        value = StringFun.run(value);
                    } catch (Exception e) {
                        log.error("字符串替换报错", e);
                        log.error(String.format("字符串: %s; map: %s", value, line));
                    }
                    key = Tools.processTemplateString(key, line);
                    newJsonObject.put(key, Tools.richDoc(value));
                }
                if (!exsitInCache(newJsonObject)) {
                    exit = false;
                }
                jsonObjectList.add(newJsonObject);
            }
            if (!exit) {
                backendData.add(jsonObjectList);
            }
        }
        return backendData;
    }

    /**
     * 根据缓存过滤
     * 
     * @param backendData
     * @return 是否存在缓存中
     */
    private boolean exsitInCache(JSONObject backendData) {
        if (backendData.keySet().isEmpty() || !backendData.keySet().contains(Constant.INNER_ID)
                || !backendData.keySet().contains(Constant.INNER_TYPE)) {
            return true;
        }
        String id = backendData.getString(Constant.INNER_ID);
        String type = backendData.getString(Constant.INNER_TYPE);
        String key = String.valueOf(id.hashCode());
        String newValue = String.valueOf(backendData.hashCode());
        String oldValue = redisHelper.hget(type, key);
        int expireTime = backendStoreConfigService.getTypeValidTime(type);
        if (0 == expireTime) {
            expireTime = CONSUME_KEY_EXPIRE_TIME;
        } else {
            expireTime /= 2;
        }
        if (newValue.equals(oldValue)) {
            return true;
        } else {
            redisHelper.hset(type, key, newValue, expireTime);
            return false;
        }
    }

    int saveToBackendStore(ConsumerDto consumerDto, List<JSONObject> lines, JSONArray template, String partition) {
        log.info(String.format("start save to backend, consumer is %s", consumerDto.getName()));
        try {
            if (CollectionUtils.isEmpty(lines)) {
                return 0;
            }
            List<List<JSONObject>> data = lines.parallelStream()
                    .map(jsonObject -> lineToBackendData(jsonObject, template)).flatMap(List::stream)
                    .collect(Collectors.toList());
            if (!lines.isEmpty()) {
                log.info(String.format("%s %s records are filtered!", consumerDto.getName(),
                        lines.size() - data.size()));
                if (!data.isEmpty()) {
                    backendStoreService.importData(consumerDto.getName(), data, partition, this.consumeStime,
                            consumerDto);
                }
                return data.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("saveToBackendStore ERROR: ", e);
            throw e;
        }
    }

}
