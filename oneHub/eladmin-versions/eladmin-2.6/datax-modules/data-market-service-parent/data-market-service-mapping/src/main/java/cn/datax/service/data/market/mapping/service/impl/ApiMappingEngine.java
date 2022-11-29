package cn.datax.service.data.market.mapping.service.impl;

import cn.datax.common.database.DataSourceFactory;
import cn.datax.common.database.DbQuery;
import cn.datax.common.database.constants.DbQueryProperty;
import cn.datax.common.database.core.PageResult;
import cn.datax.common.exception.DataException;
import cn.datax.common.utils.PageUtil;
import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.data.market.api.dto.FieldRule;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.feign.ApiMaskServiceFeign;
import cn.datax.service.data.market.mapping.factory.AbstractFactory;
import cn.datax.service.data.market.mapping.factory.FactoryProducer;
import cn.datax.service.data.market.mapping.factory.crypto.Crypto;
import cn.datax.service.data.market.mapping.utils.SqlBuilderUtil;
import cn.datax.service.data.metadata.api.dto.DbSchema;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.feign.MetadataSourceServiceFeign;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ApiMappingEngine {

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Autowired
    private MetadataSourceServiceFeign metadataSourceServiceFeign;

    @Autowired
    private ApiMaskServiceFeign apiMaskServiceFeign;

    public PageResult<Map<String, Object>> execute(DataApiEntity dataApi, Map<String, Object> params) {
        MetadataSourceEntity dataSource = Optional.ofNullable(metadataSourceServiceFeign.getMetadataSourceById(dataApi.getExecuteConfig().getSourceId())).orElseThrow(() -> new DataException("API调用查询数据源出错"));
        DbSchema dbSchema = dataSource.getDbSchema();
        DbQueryProperty dbQueryProperty = new DbQueryProperty(dataSource.getDbType(), dbSchema.getHost(),
                dbSchema.getUsername(), dbSchema.getPassword(), dbSchema.getPort(), dbSchema.getDbName(), dbSchema.getSid());
        DbQuery dbQuery = Optional.ofNullable(dataSourceFactory.createDbQuery(dbQueryProperty)).orElseThrow(() -> new DataException("创建数据查询接口出错"));
        // 参数
        Integer pageNum = Integer.parseInt((String) params.getOrDefault("pageNum", 1));
        Integer pageSize = Integer.parseInt((String) params.getOrDefault("pageSize", 20));
        PageUtil pageUtil = new PageUtil(pageNum, pageSize);
        Integer offset = pageUtil.getOffset();
        SqlBuilderUtil.SqlFilterResult sqlFilterResult;
        try {
            sqlFilterResult = SqlBuilderUtil.getInstance().applyFilters(dataApi.getExecuteConfig().getSqlText(), params);
        } catch (Exception e) {
            log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            throw new DataException("API调用动态构造SQL语句出错");
        }
        Map<String, Object> acceptedFilters = sqlFilterResult.getAcceptedFilters();
        // 数据脱敏
        List<FieldRule> rules = null;
        ApiMaskEntity apiMaskEntity = apiMaskServiceFeign.getApiMaskByApiId(dataApi.getId());
        if (apiMaskEntity != null) {
            rules = apiMaskEntity.getRules();
        }
        PageResult<Map<String, Object>> pageResult;
        try {
            pageResult = dbQuery.queryByPage(sqlFilterResult.getSql(), acceptedFilters, offset, pageSize);
        } catch (Exception e) {
            log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            throw new DataException("API调用查询结果集出错");
        }
        try {
            if (CollUtil.isNotEmpty(rules)){
                // 并行流处理脱敏
                List<FieldRule> finalRules = rules;
                pageResult.getData().parallelStream().forEach(m -> {
                    finalRules.forEach(r -> {
                        if (m.containsKey(r.getFieldName())) {
                            Object obj = m.get(r.getFieldName());
                            if (null != obj){
                                AbstractFactory factory = FactoryProducer.getFactory(r.getCipherType());
                                Crypto crypto = factory.getCrypto(r.getCryptType());
                                String encrypt = crypto.encrypt(String.valueOf(obj));
                                m.put(r.getFieldName(), encrypt);
                            }
                        }
                    });
                });
            }
        } catch (Exception e) {
            log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
            throw new DataException("API调用数据脱敏出错");
        }
        pageResult.setPageNum(pageNum).setPageSize(pageSize);
        return pageResult;
    }
}
