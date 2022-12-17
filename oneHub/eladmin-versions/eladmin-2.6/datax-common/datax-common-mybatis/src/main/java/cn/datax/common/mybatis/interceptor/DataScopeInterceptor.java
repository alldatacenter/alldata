package cn.datax.common.mybatis.interceptor;

import cn.datax.common.base.DataScope;
import cn.datax.common.core.DataConstant;
import cn.datax.common.core.DataRole;
import cn.datax.common.core.DataUser;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.system.api.dto.JwtUserDto;
import cn.datax.service.system.api.dto.UserLoginDto;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.handlers.AbstractSqlParserHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * mybatis 数据权限拦截器
 */
@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class DataScopeInterceptor extends AbstractSqlParserHandler implements Interceptor {

	@Override
	@SneakyThrows
	public Object intercept(Invocation invocation) {
		StatementHandler statementHandler = (StatementHandler) PluginUtils.realTarget(invocation.getTarget());
		MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
		this.sqlParser(metaObject);
		// 先判断是不是SELECT操作
		MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
		if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
			return invocation.proceed();
		}

		BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
		String originalSql = boundSql.getSql();
		Object parameterObject = boundSql.getParameterObject();

		//查找参数中包含DataScope类型的参数
		DataScope dataScope = findDataScopeObject(parameterObject);

		if (dataScope != null) {
			// 获取当前的用户
			JwtUserDto currentUser = SecurityUtil.getDataUser();
			if (null != currentUser) {
				UserLoginDto user = currentUser.getUser();
				// 如果是超级管理员，则不过滤数据
//				if (!user.isAdmin) {
//					String sqlString = dataScopeFilter(currentUser, dataScope);
//					if (StrUtil.isNotBlank(sqlString)) {
//						originalSql = "SELECT * FROM (" + originalSql + ") TEMP_DATA_SCOPE WHERE 1=1 AND (" + sqlString.substring(4) + ")";
//						metaObject.setValue("delegate.boundSql.sql", originalSql);
//					}
//				}
			}
		}
		return invocation.proceed();
	}

//	/**
//	 * 数据范围过滤
//	 *
//	 * @param user
//	 * @param dataScope
//	 */
//	private String dataScopeFilter(JwtUserDto user, DataScope dataScope) {
//		StringBuilder sqlString = new StringBuilder();
//		Set<String> roles = user.getRoles();
//		if (CollUtil.isNotEmpty(roles)){
//			for (String role : roles){
////				String roleDataScope = role.getDataScope();
//				if (DataConstant.DataScope.ALL.getKey().equals(roleDataScope)) {
//					sqlString = new StringBuilder();
//					break;
//				} else if (DataConstant.DataScope.CUSTOM.getKey().equals(roleDataScope)) {
//					sqlString.append(StrUtil.format(
//							" OR {} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) "
//							,dataScope.getDeptScopeName()
//							,"'" + role + "'"
//					));
//				} else if (DataConstant.DataScope.DEPT.getKey().equals(roleDataScope)) {
//					sqlString.append(StrUtil.format(
//							" OR {} = {} "
//							,dataScope.getDeptScopeName()
//							,"'" + user.getUser().getDept() + "'"
//					));
//				} else if (DataConstant.DataScope.DEPTANDCHILD.getKey().equals(roleDataScope)) {
//					sqlString.append(StrUtil.format(
//							" OR {} IN ( SELECT descendant FROM sys_dept_relation WHERE ancestor = {} )"
//							,dataScope.getDeptScopeName()
//							,"'" + user.getUser().getDept() + "'"
//					));
//				} else if (DataConstant.DataScope.SELF.getKey().equals(roleDataScope)) {
//					sqlString.append(StrUtil.format(" OR {} = {} "
//							,dataScope.getUserScopeName()
//							,"'" + user.getUser().getId() + "'"
//					));
//				}
//			}
//		}
//		log.info("数据范围过滤:{}", sqlString);
//		return sqlString.toString();
//	}

	/**
	 * 生成拦截对象的代理
	 *
	 * @param target 目标对象
	 * @return 代理对象
	 */
	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	/**
	 * mybatis配置的属性
	 *
	 * @param properties mybatis配置的属性
	 */
	@Override
	public void setProperties(Properties properties) {

	}

	/**
	 * 查找参数是否包括DataScope对象
	 *
	 * @param parameterObj 参数列表
	 * @return DataScope
	 */
	private DataScope findDataScopeObject(Object parameterObj) {
		if (parameterObj instanceof DataScope) {
			return (DataScope) parameterObj;
		} else if (parameterObj instanceof Map) {
			for (Object val : ((Map<?, ?>) parameterObj).values()) {
				if (val instanceof DataScope) {
					return (DataScope) val;
				}
			}
		}
		return null;
	}

}
