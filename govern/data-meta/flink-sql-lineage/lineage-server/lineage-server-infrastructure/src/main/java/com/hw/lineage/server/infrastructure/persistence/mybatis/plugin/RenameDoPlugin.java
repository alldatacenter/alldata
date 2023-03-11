package com.hw.lineage.server.infrastructure.persistence.mybatis.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;

import java.util.List;

/**
 * @description: RenameDomainPlugin
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class RenameDoPlugin extends PluginAdapter {

    private static final String DATA_OBJECT_SUFFIX = "DO";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        // add DO suffix to Data Object
        String recordType = introspectedTable.getBaseRecordType();
        introspectedTable.setBaseRecordType(recordType + DATA_OBJECT_SUFFIX);

        // DO suffix is not required in Mapper
        String mapperType = introspectedTable.getMyBatis3JavaMapperType();
        introspectedTable.setMyBatis3JavaMapperType(mapperType.replace(DATA_OBJECT_SUFFIX, ""));
    }
}
