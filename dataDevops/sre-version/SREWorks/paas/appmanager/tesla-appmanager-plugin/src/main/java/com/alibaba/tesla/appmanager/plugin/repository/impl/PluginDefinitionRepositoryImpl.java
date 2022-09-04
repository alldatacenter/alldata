package com.alibaba.tesla.appmanager.plugin.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.plugin.repository.PluginDefinitionRepository;
import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginDefinitionDO;
import com.alibaba.tesla.appmanager.plugin.repository.mapper.PluginDefinitionDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Plugin Repository
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PluginDefinitionRepositoryImpl implements PluginDefinitionRepository {

    @Autowired
    private PluginDefinitionDOMapper mapper;

    @Override
    public int insert(PluginDefinitionDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    private PluginDefinitionDO insertDate(PluginDefinitionDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }
}
