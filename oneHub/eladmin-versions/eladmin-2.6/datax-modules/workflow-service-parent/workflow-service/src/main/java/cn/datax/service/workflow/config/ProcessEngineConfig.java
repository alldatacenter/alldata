package cn.datax.service.workflow.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import lombok.AllArgsConstructor;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@AllArgsConstructor
public class ProcessEngineConfig {

    private final DataSource dataSource;

    /**
     * ProcessEngine 配置，注入DataSourceTransactionManager和DataSource
     * @return
     */
    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration() throws IOException {
        SpringProcessEngineConfiguration springProcessEngineConfiguration = new SpringProcessEngineConfiguration();
        // 手动从多数据源中获取 flowable数据源
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        DataSource flowable = ds.getDataSource("flowable");
        springProcessEngineConfiguration.setDataSource(flowable);
        springProcessEngineConfiguration.setTransactionManager(new DataSourceTransactionManager(flowable));
        // 是否自动创建流程引擎表
        springProcessEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        // 流程图字体
        springProcessEngineConfiguration.setActivityFontName("宋体");
        springProcessEngineConfiguration.setLabelFontName("宋体");
        springProcessEngineConfiguration.setAnnotationFontName("宋体");
        //流程历史等级
        springProcessEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        return springProcessEngineConfiguration;
    }
}
