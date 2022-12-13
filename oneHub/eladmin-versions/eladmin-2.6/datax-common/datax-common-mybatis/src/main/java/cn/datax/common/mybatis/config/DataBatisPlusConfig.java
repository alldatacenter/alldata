package cn.datax.common.mybatis.config;

import cn.datax.common.mybatis.aspectj.DataScopeAspect;
import cn.datax.common.mybatis.injector.DataLogicSqlInjector;
import cn.datax.common.mybatis.interceptor.DataScopeInterceptor;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan({"cn.datax.service.**.dao","com.platform.dts.mapper"})
@EnableTransactionManagement
public class DataBatisPlusConfig {

	/**
	 * 分页插件
	 *
	 * @return PaginationInterceptor
	 */
	@Bean
	public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
	}

	/**
	 * 数据权限插件
	 *
	 * @return DataScopeInterceptor
	 */
	@Bean
	public DataScopeInterceptor dataScopeInterceptor() {
		return new DataScopeInterceptor();
	}

	/**
	 * 数据过滤处理（基于注解式）
	 *
	 * @return dataScopeAspect
	 */
	@Bean
	public DataScopeAspect dataScopeAspect() {
		return new DataScopeAspect();
	}

	/**
	 * 自定义 SqlInjector
	 * 里面包含自定义的全局方法
	 */
	@Bean
	public DataLogicSqlInjector myLogicSqlInjector() {
		return new DataLogicSqlInjector();
	}

	@Bean
	public DataMetaObjectHandler dataMetaObjectHandler() {
		return new DataMetaObjectHandler();
	}
}
