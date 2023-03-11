package com.hw.lineage.server.infrastructure.repository.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.hw.lineage.common.enums.CatalogType;
import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.domain.entity.Function;
import com.hw.lineage.server.domain.query.function.FunctionCheck;
import com.hw.lineage.server.domain.query.function.FunctionEntry;
import com.hw.lineage.server.domain.query.function.FunctionQuery;
import com.hw.lineage.server.domain.repository.FunctionRepository;
import com.hw.lineage.server.domain.vo.FunctionId;
import com.hw.lineage.server.infrastructure.persistence.converter.DataConverter;
import com.hw.lineage.server.infrastructure.persistence.dos.FunctionDO;
import com.hw.lineage.server.infrastructure.persistence.mapper.FunctionMapper;
import com.hw.lineage.server.infrastructure.persistence.mapper.custom.CustomFunctionMapper;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

import static com.hw.lineage.server.infrastructure.persistence.mapper.CatalogDynamicSqlSupport.catalog;
import static com.hw.lineage.server.infrastructure.persistence.mapper.FunctionDynamicSqlSupport.*;
import static com.hw.lineage.server.infrastructure.persistence.mapper.PluginDynamicSqlSupport.plugin;
import static org.mybatis.dynamic.sql.SqlBuilder.*;


/**
 * @description: FunctionRepositoryImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Repository
public class FunctionRepositoryImpl extends AbstractBasicRepository implements FunctionRepository {

    @Resource
    private FunctionMapper functionMapper;

    @Resource
    private CustomFunctionMapper customFunctionMapper;

    @Resource
    private DataConverter converter;

    @Override
    public Function find(FunctionId functionId) {
        FunctionDO functionDO = functionMapper.selectByPrimaryKey(functionId.getValue())
                .orElseThrow(() ->
                        new LineageException(String.format("functionId [%s] is not existed", functionId.getValue()))
                );
        return converter.toFunction(functionDO);
    }

    @Override
    public boolean check(FunctionCheck functionCheck) {
        return !functionMapper.select(completer -> completer.where(catalogId, isEqualTo(functionCheck.getCatalogId()))
                .and(database, isEqualTo(functionCheck.getDatabase()))
                .and(functionName, isEqualTo(functionCheck.getFunctionName()))).isEmpty();
    }

    @Override
    public Function save(Function function) {
        FunctionDO functionDO = converter.fromFunction(function);
        if (functionDO.getFunctionId() == null) {
            functionMapper.insertSelective(functionDO);
        } else {
            functionMapper.updateByPrimaryKeySelective(functionDO);
        }
        return converter.toFunction(functionDO);
    }

    @Override
    public void remove(FunctionId functionId) {
        functionMapper.deleteByPrimaryKey(functionId.getValue());
    }

    @Override
    public PageInfo<Function> findAll(FunctionQuery functionQuery) {
        try (Page<FunctionDO> page = PageMethod.startPage(functionQuery.getPageNum(), functionQuery.getPageSize())) {
            PageInfo<FunctionDO> pageInfo = page.doSelectPageInfo(() ->
                    functionMapper.select(completer ->
                            completer.where(catalogId, isEqualTo(functionQuery.getCatalogId()))
                                    .and(database, isEqualTo(functionQuery.getDatabase()))
                                    .and(functionName, isLike(buildLikeValue(functionQuery.getFunctionName())))
                                    .orderBy(buildSortSpecification(functionQuery))
                    )
            );
            return PageUtils.convertPage(pageInfo, converter::toFunction);
        }
    }

    @Override
    public FunctionEntry findEntry(FunctionId functionId) {
        SelectStatementProvider selectStatement =
                select(plugin.pluginCode, catalog.catalogName, function.database, function.functionId, function.functionName)
                        .from(function)
                        .join(catalog).on(function.catalogId, equalTo(catalog.catalogId))
                        .join(plugin).on(catalog.pluginId, equalTo(plugin.pluginId))
                        .where(function.functionId, isEqualTo(functionId.getValue()))
                        .build().render(RenderingStrategies.MYBATIS3);

        return customFunctionMapper.selectOne(selectStatement).orElseThrow(() ->
                new LineageException(String.format("functionId [%s] is not existed", functionId.getValue()))
        );
    }

    @Override
    public List<Function> findMemory() {
        List<FunctionDO> functionDOList = functionMapper.select(completer ->
                completer.join(catalog).on(function.catalogId, equalTo(catalog.catalogId))
                        .where(catalog.catalogType, isEqualToWhenPresent(CatalogType.MEMORY))
        );
        return converter.toFunctionList(functionDOList);
    }

}
