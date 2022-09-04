package com.alibaba.tesla.tkgone.server.services.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Cache;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.RedisHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.Config;
import com.alibaba.tesla.tkgone.server.domain.ConfigExample;
import com.alibaba.tesla.tkgone.server.domain.ConfigMapper;
import com.alibaba.tesla.tkgone.server.domain.dto.ConfigDto;
import com.alibaba.tesla.tkgone.server.services.database.elasticsearch.ElasticSearchIndicesService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础配置为一张表：category, type(type), id, name 四个字段 提供对基础配置的 增、删、改和查 功能 值为content
 * 修改人为modifier
 *
 * @author yangjinghua
 */
@Service
@Log4j
@Component("baseConfigService")
public class BaseConfigService {

    @Autowired
    public ConfigMapper configMapper;

    @Autowired
    public CategoryConfigService categoryConfigService;

    @Autowired
    public ElasticSearchIndicesService elasticSearchIndicesService;

    @Autowired
    RedisHelper redisHelper;

    public ConfigExample getExampleByConfigDto(ConfigDto configDto) {
        ConfigExample example = new ConfigExample();
        ConfigExample.Criteria criteria = example.createCriteria();
        if (configDto.getCategory() != null) {
            criteria.andCategoryEqualTo(configDto.getCategory());
        }
        if (configDto.getNrType() != null) {
            criteria.andNrTypeEqualTo(configDto.getNrType());
        }
        if (configDto.getNrId() != null) {
            criteria.andNrIdEqualTo(configDto.getNrId());
        }
        if (configDto.getName() != null) {
            criteria.andNameEqualTo(configDto.getName());
        }
        return example;
    }

    public List<ConfigDto> select(ConfigDto configDto) {

        ConfigExample example = getExampleByConfigDto(configDto);
        return configMapper.selectByExample(example).stream().map(ConfigDto::new).collect(Collectors.toList());

    }

    private int upsert(ConfigDto configDto) {

        configDto.setGmtModified(new Date());
        ConfigExample example = getExampleByConfigDto(configDto);
        List<Config> configList = configMapper.selectByExample(example);
        if (configList.size() == 0) {
            configDto.setGmtCreate(new Date());
            return configMapper.insert(configDto.toConfig());
        } else {
            return configMapper.updateByExampleSelective(configDto, example);
        }

    }

    public int delete(ConfigDto configDto) {
        ConfigExample example = getExampleByConfigDto(configDto);
        return configMapper.deleteByExample(example);
    }

    public int deleteById(Long id) {
        return configMapper.deleteByPrimaryKey(id);
    }

    public List<ConfigDto> getConfigDtoList(ConfigDto configDto) {
        final String[] filters = new String[] { "prod_user", "prod_service" };
        List<String> keys = new ArrayList<>(Arrays.asList(configDto.getCategory(), configDto.getNrType(),
                configDto.getNrId(), configDto.getName()));
        while (keys.contains(null) || keys.contains("")) {
            keys.removeAll(Arrays.asList(null, ""));
        }
        if (keys.isEmpty()) {
            List<Config> configs = configMapper.selectByExample(new ConfigExample());
            return configs.stream().filter(config -> {
                for (String filterStr : filters) {
                    if (config.getName().startsWith(filterStr)) {
                        return false;
                    }
                }
                if ("sys".equals(config.getModifier())) {
                    return false;
                }
                return true;
            }).map(config -> new ConfigDto(config)).collect(Collectors.toList());
        }
        String name = String.join(":", keys);
        if (Cache.allConfigDto.containsKey(name)) {
            return Cache.allConfigDto.get(name);
        } else {
            return new ArrayList<>();
        }
    }

    private String getContentWithCache(ConfigDto configDto) {
        List<ConfigDto> configDtoList = getConfigDtoList(configDto);
        if (CollectionUtils.isEmpty(configDtoList)) {
            return null;
        } else {
            return configDtoList.get(0).getContent();
        }

    }

    public String getContentWithOutCache(ConfigDto configDto) {
        List<ConfigDto> configDtoList = select(configDto);
        return CollectionUtils.isEmpty(configDtoList) ? "" : configDtoList.get(0).getContent();

    }

    private String getContent(ConfigDto configDto) {
        return getContentWithCache(configDto);

    }

    /**
     * 写入content
     */
    public int setCategoryTypeIdNameContent(String category, String type, String id, String name, Object content,
            String modifier) {
        return upsert(ConfigDto.builder().category(category).nrType(type).nrId(id).name(name)
                .content(Tools.objectToString(content)).modifier(modifier).build());
    }

    public int setCategoryTypeNameContent(String category, String type, String name, Object content, String modifier) {
        return upsert(ConfigDto.builder().category(category).nrType(type).nrId(Constant.DEFAULT_NR_ID).name(name)
                .content(Tools.objectToString(content)).modifier(modifier).build());
    }

    public int setTypeIdNameContent(String type, String id, String name, Object content, String modifier) {
        return upsert(ConfigDto.builder().category(Constant.DEFAULT_CATEGORY).nrType(type).nrId(id).name(name)
                .content(Tools.objectToString(content)).modifier(modifier).build());
    }

    public int setCategoryNameContent(String category, String name, Object content, String modifier) {
        return upsert(
                ConfigDto.builder().category(category).nrType(Constant.DEFAULT_NR_TYPE).nrId(Constant.DEFAULT_NR_ID)
                        .name(name).content(Tools.objectToString(content)).modifier(modifier).build());
    }

    public int setTypeNameContent(String type, String name, Object content, String modifier) {
        return upsert(ConfigDto.builder().category(Constant.DEFAULT_CATEGORY).nrType(type).nrId(Constant.DEFAULT_NR_ID)
                .name(name).content(Tools.objectToString(content)).modifier(modifier).build());
    }

    public int setNameContent(String name, Object content, String modifier) {
        return upsert(ConfigDto.builder().category(Constant.DEFAULT_CATEGORY).nrType(Constant.DEFAULT_NR_TYPE)
                .nrId(Constant.DEFAULT_NR_ID).name(name).content(Tools.objectToString(content)).modifier(modifier)
                .build());
    }

    /**
     * 获取content
     */

    // 以下三个方法都是category内部配置映射，并且不会采用默认（基础）category的配置
    public String getCategoryTypeIdNameContentWithCategoryWithOutExtend(String category, String type, String id,
            String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;
        id = StringUtils.isEmpty(id) ? Constant.DEFAULT_NR_ID : id;
        String content;
        content = getContent(ConfigDto.builder().category(category).nrType(type).nrId(id).name(name).build());

        if (content == null) {
            content = getCategoryTypeNameContentWithCategoryWithOutExtend(category, type, name);
        }

        return content;
    }

    public String getCategoryTypeNameContentWithCategoryWithOutExtend(String category, String type, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;

        String content;
        content = getContent(
                ConfigDto.builder().category(category).nrType(type).nrId(Constant.DEFAULT_NR_ID).name(name).build());
        if (content == null) {
            content = getCategoryNameContentWithCategoryWithOutExtend(category, name);
        }

        return content;
    }

    public String getCategoryNameContentWithCategoryWithOutExtend(String category, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        return getContent(ConfigDto.builder().category(category).nrType(Constant.DEFAULT_NR_TYPE)
                .nrId(Constant.DEFAULT_NR_ID).name(name).build());
    }

    // 以下三个方法是category继承的方法调用，除非明确该category从默认(基础)category继承下来，否则不会采用默认(基础)配置
    public String getCategoryTypeIdNameContentWithCategory(String category, String type, String id, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;
        id = StringUtils.isEmpty(id) ? Constant.DEFAULT_NR_ID : id;
        String content = null;
        while (!StringUtils.isEmpty(category)) {
            content = getCategoryTypeIdNameContentWithCategoryWithOutExtend(category, type, id, name);
            if (content == null) {
                category = categoryConfigService.getCategoryExtendCategory(category);
            } else {
                break;
            }
        }
        return content;
    }

    public String getCategoryTypeNameContentWithCategory(String category, String type, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;
        String content = null;
        while (!StringUtils.isEmpty(category)) {
            content = getCategoryTypeNameContentWithCategoryWithOutExtend(category, type, name);
            if (content == null) {
                category = categoryConfigService.getCategoryExtendCategory(category);
            } else {
                break;
            }
        }
        return content;
    }

    public String getCategoryNameContentWithCategory(String category, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        String content = null;
        while (!StringUtils.isEmpty(category)) {
            content = getCategoryNameContentWithCategoryWithOutExtend(category, name);
            if (content == null) {
                category = categoryConfigService.getCategoryExtendCategory(category);
            } else {
                break;
            }
        }
        return content;
    }

    // 以下三个方法都是category内部配置映射，会采用默认（基础）category的配置
    public String getCategoryTypeIdNameContentWithOutExtend(String category, String type, String id, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;
        id = StringUtils.isEmpty(id) ? Constant.DEFAULT_NR_ID : id;
        String content = getCategoryTypeIdNameContentWithCategoryWithOutExtend(category, type, id, name);

        if (content == null) {
            content = getCategoryTypeNameContentWithOutExtend(category, type, name);
        }

        if (content == null) {
            content = getTypeIdNameContent(type, id, name);
        }

        return content;
    }

    public String getCategoryTypeNameContentWithOutExtend(String category, String type, String name) {
        category = StringUtils.isEmpty(category) ? Constant.DEFAULT_CATEGORY : category;
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;

        String content = getCategoryTypeNameContentWithCategory(category, type, name);

        // 比如说默认的分区数量以及超时时间，category级别可以覆盖设置，比type级别的通用设置的优先级更高
        if (content == null) {
            content = getCategoryNameContentWithOutExtend(category, name);
        }

        if (content == null) {
            content = getTypeNameContent(type, name);
        }

        return content;
    }

    public String getCategoryNameContentWithOutExtend(String category, String name) {
        String content = getCategoryNameContentWithCategoryWithOutExtend(category, name);
        if (content == null) {
            content = getNameContent(name);
        }
        return content;
    }

    // 以下三个方法是全量映射配置，优先使用category内部的配置，其次使用父亲category的配置，最终使用默认(基础)配置
    public String getCategoryTypeIdNameContent(String category, String type, String id, String name) {
        String content = getCategoryTypeIdNameContentWithCategory(category, type, id, name);
        if (content == null) {
            content = getCategoryTypeIdNameContentWithCategory(category, type, id, name);
        }
        return content;
    }

    public String getCategoryTypeNameContent(String category, String type, String name) {
        String content = getCategoryTypeNameContentWithCategory(category, type, name);
        if (content == null) {
            content = getCategoryTypeNameContentWithOutExtend(category, type, name);
        }
        return content;
    }

    public String getCategoryNameContent(String category, String name) {
        String content = getCategoryNameContentWithCategory(category, name);
        if (content == null) {
            content = getCategoryNameContentWithOutExtend(category, name);
        }
        return content;
    }

    // 以下方法都是没有category的配置获取，即默认(基础)配置
    public String getTypeIdNameContent(String type, String id, String name) {
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;
        id = StringUtils.isEmpty(id) ? Constant.DEFAULT_NR_ID : id;

        String content;
        content = getContent(
                ConfigDto.builder().category(Constant.DEFAULT_CATEGORY).nrType(type).nrId(id).name(name).build());

        if (content == null) {
            content = getTypeNameContent(type, name);
        }
        return content;
    }

    public String getTypeNameContent(String type, String name) {
        type = StringUtils.isEmpty(type) ? Constant.DEFAULT_NR_TYPE : type;
        ConfigDto configDto = ConfigDto.builder().category(Constant.DEFAULT_CATEGORY).nrType(type)
                .nrId(Constant.DEFAULT_NR_ID).name(name).build();
        String content = getContent(configDto);
        if (content == null) {
            content = getNameContent(name);
        }
        return content;
    }

    public String getNameContent(String name) {
        return getContent(ConfigDto.builder()
            .category(Constant.DEFAULT_CATEGORY)
            .nrType(Constant.DEFAULT_NR_TYPE)
            .nrId(Constant.DEFAULT_NR_ID)
            .name(name)
            .build());
    }

    /**
     * getContentWithDefaultValue
     */

    private JSONArray getContentWithDefault(String content, JSONArray defaultArray) {
        if (StringUtils.isEmpty(content)) {
            return defaultArray;
        } else {
            try {
                return JSONObject.parseArray(content);
            } catch (Exception e) {
                log.error("获取JSONArray类型的配置时，发现配置结构不正确，返回默认值并报错: ", e);
                return defaultArray;
            }
        }
    }

    private JSONObject getContentWithDefault(String content, JSONObject defaultObject) {
        if (StringUtils.isEmpty(content)) {
            return defaultObject;
        } else {
            try {
                return JSONObject.parseObject(content);
            } catch (Exception e) {
                log.error("获取JSONObject类型的配置时，发现配置结构不正确，返回默认值并报错: ", e);
                return defaultObject;
            }
        }
    }

    private List<String> getContentWithDefault(String content, List<String> defaultList) {
        if (StringUtils.isEmpty(content)) {
            return defaultList;
        } else {
            try {
                return JSONObject.parseArray(content).toJavaList(String.class);
            } catch (Exception e) {
                log.error("获取JSONObject类型的配置时，发现配置结构不正确，返回默认值并报错: ", e);
                return defaultList;
            }
        }
    }

    private String getContentWithDefault(String content, String defaultString) {
        if (StringUtils.isEmpty(content)) {
            return defaultString;
        } else {
            return content;
        }
    }

    private int getContentWithDefault(String content, int defaultInteger) {
        if (Tools.isNumber(content)) {
            return Integer.parseInt(content);
        } else {
            return defaultInteger;
        }
    }

    private long getContentWithDefault(String content, long defaultInteger) {
        if (Tools.isNumber(content)) {
            return Long.parseLong(content);
        } else {
            return defaultInteger;
        }
    }

    /* 各种带有defaultValue的查询 */

    public JSONArray getCategoryTypeIdNameContentWithDefault(String category, String type, String id, String name,
            JSONArray defaultArray) {
        String content = getCategoryTypeIdNameContent(category, type, id, name);
        return getContentWithDefault(content, defaultArray);
    }

    public String getCategoryTypeIdNameContentWithDefault(String category, String type, String id, String name,
            String defaultString) {
        String content = getCategoryTypeIdNameContent(category, type, id, name);
        return getContentWithDefault(content, defaultString);
    }

    public int getCategoryTypeIdNameContentWithDefault(String category, String type, String id, String name,
            int defaultIntValue) {
        String content = getCategoryTypeIdNameContent(category, type, id, name);
        return getContentWithDefault(content, defaultIntValue);
    }

    public String getCategoryTypeNameContentWithDefault(String category, String type, String name,
            String defaultValue) {
        String content = getCategoryTypeNameContent(category, type, name);
        return getContentWithDefault(content, defaultValue);
    }

    public JSONArray getCategoryTypeNameContentWithDefault(String category, String type, String name,
            JSONArray defaultArray) {
        String content = getCategoryTypeNameContent(category, type, name);
        return getContentWithDefault(content, defaultArray);
    }

    public JSONObject getCategoryTypeNameContentWithDefault(String category, String type, String name,
            JSONObject defaultJson) {
        String content = getCategoryTypeNameContent(category, type, name);
        return getContentWithDefault(content, defaultJson);
    }


    public int getCategoryNameContentWithDefault(String category, String name, int defaultInt) {
        String content = getCategoryNameContent(category, name);
        return getContentWithDefault(content, defaultInt);
    }

    public JSONArray getCategoryNameContentWithDefault(String category, String name, JSONArray defaultValue) {
        String content = getCategoryNameContent(category, name);
        return getContentWithDefault(content, defaultValue);
    }

    public JSONObject getCategoryNameContentWithDefault(String category, String name, JSONObject defaultJson) {
        String content = getCategoryNameContent(category, name);
        return getContentWithDefault(content, defaultJson);
    }

    public JSONArray getTypeIdNameContentWithDefault(String type, String nrId, String name, JSONArray defaultArray) {
        String content = getTypeIdNameContent(type, nrId, name);
        return getContentWithDefault(content, defaultArray);
    }

    public Long getTypeNameContentWithDefault(String type, String name, Long defaultValue) {
        String content = getTypeNameContent(type, name);
        return getContentWithDefault(content, defaultValue);
    }

    public JSONObject getTypeNameContentWithDefault(String type, String name, JSONObject defaultJson) {
        String content = getTypeNameContent(type, name);
        return getContentWithDefault(content, defaultJson);
    }

    public int getTypeNameContentWithDefault(String type, String name, int defaultValue) {
        String content = getTypeNameContent(type, name);
        return getContentWithDefault(content, defaultValue);
    }

    public String getTypeNameContentWithDefault(String type, String name, String defaultValue) {
        String content = getTypeNameContent(type, name);
        return getContentWithDefault(content, defaultValue);
    }

    public List<String> getTypeNameContentWithDefault(String type, String name, List<String> defaultList) {
        String content = getTypeNameContent(type, name);
        return getContentWithDefault(content, defaultList);
    }

    public JSONArray getTypeNameContentWithDefault(String type, String name, JSONArray jsonArray) {
        String content = getTypeNameContent(type, name);
        return getContentWithDefault(content, jsonArray);
    }

    public List<String> getNameContentWithDefault(String name, List<String> defaultValue) {
        String content = getNameContent(name);
        return getContentWithDefault(content, defaultValue);
    }

    public JSONArray getNameContentWithDefault(String name, JSONArray defaultValue) {
        String content = getNameContent(name);
        return getContentWithDefault(content, defaultValue);
    }

    public JSONObject getNameContentWithDefault(String name, JSONObject defaultJson) {
        String content = getNameContent(name);
        return getContentWithDefault(content, defaultJson);
    }

    public Integer getNameContentWithDefault(String name, int defaultValue) {
        String content = getNameContent(name);
        return getContentWithDefault(content, defaultValue);
    }

    public String getNameContentWithDefault(String name, String defaultValue) {
        String content = getNameContent(name);
        return getContentWithDefault(content, defaultValue);
    }

}
