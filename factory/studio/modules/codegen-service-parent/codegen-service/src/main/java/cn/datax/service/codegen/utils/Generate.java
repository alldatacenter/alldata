package cn.datax.service.codegen.utils;

import cn.datax.service.codegen.engine.VelocityTemplateEngine;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.io.File;
import java.util.*;

public class Generate {

//    public static void main(String[] args) throws InterruptedException {
////        DefaultIdentifierGenerator generator = new DefaultIdentifierGenerator();
////        for (int i = 0; i < 10; i++) {
////            Thread.sleep(new Random().nextInt(1000 - 500 + 1) + 500);
////            System.out.println(generator.nextId(null));
////        }
//        generateByTables("F://code", "visual", "cn.datax.service.data", "visual_", new String[]{"visual_board_chart"});
//    }

    /**
     * 根据表自动生成
     *
     * @param moduleName            模块名
     * @param parentName            包名
     * @param tableNames            表名
     */
    private static void generateByTables(String projectPath, String moduleName, String parentName, String tablePrefix, String[] tableNames) {
        //配置数据源
        DataSourceConfig dataSourceConfig = getDataSourceConfig();
        //全局变量配置
        GlobalConfig globalConfig = getGlobalConfig(projectPath);
        //包名配置
        PackageConfig packageConfig = getPackageConfig(moduleName, parentName);
        // 策略配置
        StrategyConfig strategyConfig = getStrategyConfig(tablePrefix, tableNames);
        //自定义配置
        InjectionConfig injectionConfig = getInjectionConfig(projectPath, moduleName, parentName);
        //配置模板
        TemplateConfig templateConfig = getTemplateConfig();
        //自动生成
        atuoGenerator(dataSourceConfig, strategyConfig, globalConfig, packageConfig, injectionConfig, templateConfig);
    }

    /**
     * 集成
     *
     * @param dataSourceConfig 数据源配置
     * @param strategyConfig   策略配置
     * @param globalConfig     全局变量配置
     * @param packageConfig    包名配置
     * @param injectionConfig  自定义配置
     * @param templateConfig   模板配置
     */
    private static void atuoGenerator(DataSourceConfig dataSourceConfig, StrategyConfig strategyConfig, GlobalConfig globalConfig, PackageConfig packageConfig,
                               InjectionConfig injectionConfig, TemplateConfig templateConfig) {
        new AutoGenerator()
                .setGlobalConfig(globalConfig)
                .setDataSource(dataSourceConfig)
                .setStrategy(strategyConfig)
                .setPackageInfo(packageConfig)
                .setCfg(injectionConfig)
                .setTemplate(templateConfig)
                .setTemplateEngine(new VelocityTemplateEngine())
                .execute();
    }

    /**
     * 自定义配置 可以在 VM 中使用 cfg.abc
     *
     * @return templateConfig
     */
    private static InjectionConfig getInjectionConfig(String projectPath, String moduleName, String parentName) {
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                Map<String, Object> map = new HashMap<>();
                map.put("PackageParent", parentName);
                this.setMap(map);
            }
        };
        List<FileOutConfig> focList = new ArrayList<>();
        focList.add(new FileOutConfig("/templates/mapper.xml.vm") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/resources/mapper/" + tableInfo.getEntityName().replace("Entity", "Mapper") + StringPool.DOT_XML;
            }
        });
        focList.add(new FileOutConfig("/templates/entity.java.vm") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/java/"+ parentName.replace(".", File.separator) + File.separator + moduleName + "/api/entity/" + tableInfo.getEntityName() + StringPool.DOT_JAVA;
            }
        });
        focList.add(new FileOutConfig("/templates/mapstruct.java.vm") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/java/"+ parentName.replace(".", File.separator) + File.separator + moduleName + "/mapstruct/" + tableInfo.getEntityName().replace("Entity", "Mapper") + StringPool.DOT_JAVA;
            }
        });
        focList.add(new FileOutConfig("/templates/dto.java.vm") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/java/"+ parentName.replace(".", File.separator) + File.separator + moduleName + "/api/dto/" + tableInfo.getEntityName().replace("Entity", "Dto") + StringPool.DOT_JAVA;
            }
        });
        focList.add(new FileOutConfig("/templates/vo.java.vm") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/java/"+ parentName.replace(".", File.separator) + File.separator + moduleName + "/api/vo/" + tableInfo.getEntityName().replace("Entity", "Vo") + StringPool.DOT_JAVA;
            }
        });
        focList.add(new FileOutConfig("/templates/query.java.vm") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/java/"+ parentName.replace(".", File.separator) + File.separator + moduleName + "/api/query/" + tableInfo.getEntityName().replace("Entity", "Query") + StringPool.DOT_JAVA;
            }
        });
        cfg.setFileOutConfigList(focList);
        return cfg;
    }

    /**
     * 模板配置
     *
     * @return templateConfig
     */
    private static TemplateConfig getTemplateConfig() {
        return new TemplateConfig()
                .setEntity(null)
                .setController("templates/controller.java")
                .setService("templates/service.java")
                .setServiceImpl("templates/serviceImpl.java")
                .setMapper("templates/mapper.java")
                .setXml(null);
    }

    /**
     * 设置包名
     *
     * @param moduleName 模块名 如：system
     * @param parentName 父路径包名 如：cn.datax.service
     * @return PackageConfig 包名配置
     */
    private static PackageConfig getPackageConfig(String moduleName, String parentName) {
        return new PackageConfig()
                .setModuleName(moduleName)
                .setParent(parentName)
                .setXml("mapper")
                .setMapper("dao")
                .setController("controller")
                .setService("service")
                .setServiceImpl("service.impl")
                .setEntity("entity");
    }

    /**
     * 全局配置
     *
     * @return GlobalConfig
     */
    private static GlobalConfig getGlobalConfig(String projectPath) {
        return new GlobalConfig()
                //是否打开输出目录
                .setOpen(true)
                //开启 baseColumnList
                .setBaseColumnList(true)
                //开启 BaseResultMap
                .setBaseResultMap(true)
                //开启 ActiveRecord 模式
                .setActiveRecord(false)
                //是否覆盖已有文件
                .setFileOverride(true)
                //实体属性 Swagger2 注解
                .setSwagger2(false)
                .setAuthor("yuwei")
                //指定生成的主键的ID类型
                .setIdType(IdType.ASSIGN_ID)
                //设置输出路径
                .setOutputDir(projectPath + "/src/main/java/")
                .setEntityName("%sEntity")
                .setMapperName("%sDao")
                .setXmlName("%sMapper")
                .setServiceName("%sService")
                .setServiceImplName("%sServiceImpl")
                .setControllerName("%sController");
    }

    /**
     * 策略配置
     *
     * @param tableNames 表名
     * @return StrategyConfig
     */
    private static StrategyConfig getStrategyConfig(String tablePrefix, String[] tableNames) {
        StrategyConfig strategyConfig = new StrategyConfig()
                //从数据库表到文件的命名策略
                .setNaming(NamingStrategy.underline_to_camel)
                .setColumnNaming(NamingStrategy.underline_to_camel)
                .setEntityLombokModel(true)
                .setRestControllerStyle(true)
                .setControllerMappingHyphenStyle(true)
                //表前缀
                .setTablePrefix(tablePrefix)
                //写于父类中的公共字段
                .setSuperEntityColumns(new String[]{"id", "create_time", "create_by", "create_dept", "update_time", "update_by", "status", "remark"})
                .setInclude(tableNames)
                //公共父类
                .setSuperEntityClass("cn.datax.common.base.BaseEntity")
                .setSuperServiceClass("cn.datax.common.base.BaseService")
                .setSuperServiceImplClass("cn.datax.common.base.BaseServiceImpl")
                .setSuperMapperClass("cn.datax.common.base.BaseDao");
        strategyConfig.setSuperControllerClass("cn.datax.common.base.BaseController");
        return strategyConfig;
    }

    /**
     * 配置数据源
     *
     * @return 数据源配置 DataSourceConfig
     */
    private static DataSourceConfig getDataSourceConfig() {
        return new DataSourceConfig()
                .setUrl("jdbc:mysql://localhost:3306/studio?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8")
                .setDriverName("com.mysql.cj.jdbc.Driver")
                .setUsername("root")
                .setPassword("1234@abcd");
    }
}
