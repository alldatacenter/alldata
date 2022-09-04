package com.alibaba.tesla.appmanager.dynamicscript.service.impl;

import com.alibaba.tesla.appmanager.dynamicscript.repository.DynamicScriptHistoryRepository;
import com.alibaba.tesla.appmanager.dynamicscript.repository.DynamicScriptRepository;
import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDO;
import com.alibaba.tesla.appmanager.dynamicscript.service.DynamicScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 动态脚本服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DynamicScriptServiceImpl implements DynamicScriptService {

    @Autowired
    private DynamicScriptRepository dynamicScriptRepository;

    @Autowired
    private DynamicScriptHistoryRepository dynamicScriptHistoryRepository;

    /**
     * 获取动态脚本内容
     *
     * @param condition 查询条件
     * @return DynamicScriptDO
     */
    @Override
    public DynamicScriptDO get(DynamicScriptQueryCondition condition) {
        return dynamicScriptRepository.getByCondition(condition);
    }

    /**
     * 获取动态脚本列表
     *
     * @param condition 查询条件
     * @return List of DynamicScriptDO
     */
    @Override
    public List<DynamicScriptDO> list(DynamicScriptQueryCondition condition) {
        return dynamicScriptRepository.selectByCondition(condition);
    }

    /**
     * 初始化脚本
     * <p>
     * * 如果当前记录不存在，则新增
     * * 如果 version 大于当前版本，则覆盖数据库中的已有脚本数据
     * * 如果 version 小于等于当前版本，不进行操作
     *
     * @param condition 查询条件
     * @param revision   提供的脚本版本
     * @param code      Groovy 代码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initScript(DynamicScriptQueryCondition condition, Integer revision, String code) {
        String kind = condition.getKind();
        String name = condition.getName();
        DynamicScriptDO script = get(condition);
        if (script != null && script.getCurrentRevision() >= revision) {
            log.info("no need to update dynamic script|kind={}|name={}|revision={}", kind, name, revision);
            return;
        } else if (script == null) {
            dynamicScriptRepository.insert(DynamicScriptDO.builder()
                .kind(kind)
                .name(name)
                .currentRevision(revision)
                .code(code)
                .build());
            log.info("dynamic script has inserted into database|kind={}|name={}|revision={}", kind, name, revision);
        } else {
            script.setCurrentRevision(revision);
            script.setCode(code);
            dynamicScriptRepository.updateByPrimaryKey(script);
            log.info("dynamic script has updated in database|kind={}|name={}|revision={}", kind, name, revision);
        }

        // 插入历史数据
        dynamicScriptHistoryRepository.insert(DynamicScriptHistoryDO.builder()
            .kind(kind)
            .name(name)
            .revision(revision)
            .code(code)
            .build());
    }
}
