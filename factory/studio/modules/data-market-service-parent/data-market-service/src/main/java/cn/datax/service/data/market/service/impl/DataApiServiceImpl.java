package cn.datax.service.data.market.service.impl;

import cn.datax.common.base.BaseServiceImpl;
import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.rabbitmq.config.RabbitMqConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.MD5Util;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.data.market.api.dto.DataApiDto;
import cn.datax.service.data.market.api.dto.ReqParam;
import cn.datax.service.data.market.api.dto.ResParam;
import cn.datax.service.data.market.api.dto.SqlParseDto;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.enums.ConfigType;
import cn.datax.service.data.market.api.vo.ApiHeader;
import cn.datax.service.data.market.api.vo.SqlParseVo;
import cn.datax.service.data.market.dao.ApiMaskDao;
import cn.datax.service.data.market.dao.DataApiDao;
import cn.datax.service.data.market.mapstruct.DataApiMapper;
import cn.datax.service.data.market.service.DataApiService;
import cn.datax.service.data.market.utils.SqlBuilderUtil;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.enums.DataLevel;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.SelectUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 数据API信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DataApiServiceImpl extends BaseServiceImpl<DataApiDao, DataApiEntity> implements DataApiService {

	@Autowired
	private DataApiDao dataApiDao;

	@Autowired
	private ApiMaskDao apiMaskDao;

	@Autowired
	private DataApiMapper dataApiMapper;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RedisService redisService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveDataApi(DataApiDto dataApiDto) {
		DataApiEntity dataApi = shareCode(dataApiDto);
		dataApiDao.insert(dataApi);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateDataApi(DataApiDto dataApiDto) {
		DataApiEntity dataApi = shareCode(dataApiDto);
		dataApiDao.updateById(dataApi);
	}

	private DataApiEntity shareCode(DataApiDto dataApiDto) {
		DataApiEntity dataApi = dataApiMapper.toEntity(dataApiDto);
		String configType = dataApi.getExecuteConfig().getConfigType();
		if (ConfigType.FORM.getKey().equals(configType)) {
			try {
				dataApi.getExecuteConfig().setSqlText(sqlJdbcNamedParameterBuild(dataApi));
			} catch (JSQLParserException e) {
				log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
				throw new DataException("SQL语法有问题，解析出错");
			}
		} else if (ConfigType.SCRIPT.getKey().equals(configType)) {
		}
		return dataApi;
	}

	@Override
	public DataApiEntity getDataApiById(String id) {
		DataApiEntity dataApiEntity = super.getById(id);
		return dataApiEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteDataApiById(String id) {
		//zrx 判断有无脱敏依赖
		ApiMaskEntity apiMaskEntity = apiMaskDao.selectOne(new QueryWrapper<ApiMaskEntity>().eq("api_id", id).last("limit 1"));
		if (apiMaskEntity != null) {
			throw new RuntimeException("存在与之关联的数据脱敏服务，不允许删除！");
		}
		dataApiDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteDataApiBatch(List<String> ids) {
		//zrx check
		for (String id : ids) {
			deleteDataApiById(id);
		}
		//dataApiDao.deleteBatchIds(ids);
	}

	@Override
	public SqlParseVo sqlParse(SqlParseDto sqlParseDto) {
		String sourceId = sqlParseDto.getSourceId();
		String sql = sqlParseDto.getSqlText();
		sql = sql.replace(SqlBuilderUtil.getInstance().MARK_KEY_START, "");
		sql = sql.replace(SqlBuilderUtil.getInstance().MARK_KEY_END, "");
		Statement stmt;
		try {
			stmt = CCJSqlParserUtil.parse(sql);
		} catch (JSQLParserException e) {
			log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
			throw new DataException("SQL语法有问题，解析出错");
		}
		// 维护元数据缓存数据
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		List<String> tables = tablesNamesFinder.getTableList(stmt);
		// 查询字段
		final List<Map<String, String>> cols = new ArrayList<>();
		// 查询参数
		final List<String> vars = new ArrayList<>();
		if (tables.size() == 1) {
			// 单表解析
			singleSqlParse(stmt, cols, vars, tables.get(0));
		} else if (tables.size() > 1) {
			// 多表解析
			multipleSqlParse(stmt, cols, vars);
		}
		SqlParseVo sqlParseVo = new SqlParseVo();
		List<ReqParam> reqParams = vars.stream().map(s -> {
			ReqParam reqParam = new ReqParam();
			reqParam.setParamName(s);
			reqParam.setNullable(DataConstant.TrueOrFalse.FALSE.getKey());
			return reqParam;
		}).collect(Collectors.toList());
		sqlParseVo.setReqParams(reqParams);
		List<ResParam> resParams = new ArrayList<>();
		List<MetadataSourceEntity> sourceEntityList = (List<MetadataSourceEntity>) redisService.get(RedisConstant.METADATA_SOURCE_KEY);
		MetadataSourceEntity sourceEntity = sourceEntityList.stream().filter(s -> sourceId.equals(s.getId())).findFirst().orElse(null);
		boolean admin = SecurityUtil.isAdmin();
		if (sourceEntity != null) {
			List<MetadataTableEntity> tableEntityList = (List<MetadataTableEntity>) redisService.hget(RedisConstant.METADATA_TABLE_KEY, sourceEntity.getId());
			Map<String, List<Map<String, String>>> map = cols.stream().collect(Collectors.groupingBy(e -> e.get("tableName").toString()));
			for (Map.Entry<String, List<Map<String, String>>> entry : map.entrySet()) {
				String entryKey = entry.getKey().toLowerCase();
				List<Map<String, String>> entryValue = entry.getValue();
				MetadataTableEntity tableEntity = tableEntityList.stream().filter(t -> entryKey.equals(t.getTableName().toLowerCase())).findFirst().orElse(null);
				if (tableEntity != null) {
					List<MetadataColumnEntity> columnEntityList = (List<MetadataColumnEntity>) redisService.hget(RedisConstant.METADATA_COLUMN_KEY, tableEntity.getId());
					entryValue.stream().forEach(m -> {
						String columnName = m.get("columnName").toLowerCase();
						String columnAliasName = m.get("columnAliasName");
						Stream<MetadataColumnEntity> stream = columnEntityList.stream().filter(c -> columnName.equals(c.getColumnName().toLowerCase()));
						if (!admin) {
							Set<String> set = new HashSet<>();
							List<String> roleIds = SecurityUtil.getUserRoleIds();
							roleIds.stream().forEach(role -> {
								List<MetadataAuthorizeEntity> list = (List<MetadataAuthorizeEntity>) redisService.hget(RedisConstant.METADATA_AUTHORIZE_KEY, role);
								set.addAll(Optional.ofNullable(list).orElseGet(ArrayList::new).stream()
										.filter(s -> Objects.equals(DataLevel.COLUMN.getKey(), s.getObjectType()))
										.map(s -> s.getObjectId()).collect(Collectors.toSet()));
							});
							stream = stream.filter(s -> set.contains(s.getId()));
						}
						MetadataColumnEntity columnEntity = stream.findFirst().orElse(null);
						if (columnEntity != null) {
							ResParam resParam = new ResParam();
							resParam.setFieldName(columnEntity.getColumnName());
							resParam.setFieldComment(StrUtil.isNotBlank(columnEntity.getColumnComment()) ? columnEntity.getColumnComment() : "");
							resParam.setDataType(StrUtil.isNotBlank(columnEntity.getDataType()) ? columnEntity.getDataType() : "");
							resParam.setFieldAliasName(StrUtil.isNotBlank(columnAliasName) ? columnAliasName : "");
							resParams.add(resParam);
						}
					});
				}
			}
		}
		sqlParseVo.setResParams(resParams);
		return sqlParseVo;
	}

	private void singleSqlParse(Statement stmt, List<Map<String, String>> cols, List<String> vars, String tableName) {
		stmt.accept(new StatementVisitorAdapter() {
			@Override
			public void visit(Select select) {
				select.getSelectBody().accept(new SelectVisitorAdapter() {
					@Override
					public void visit(PlainSelect plainSelect) {
						plainSelect.getSelectItems().stream().forEach(selectItem -> {
							selectItem.accept(new SelectItemVisitorAdapter() {
								@Override
								public void visit(SelectExpressionItem item) {
									Map<String, String> map = new HashMap<>();
									String columnName;
									SimpleNode node = item.getExpression().getASTNode();
									Object value = node.jjtGetValue();
									if (value instanceof Column) {
										Column column = (Column) value;
										columnName = column.getColumnName();
										if (item.getAlias() != null) {
											map.put("columnAliasName", item.getAlias().getName());
										}
									} else if (value instanceof Function) {
										columnName = value.toString();
									} else {
										// 增加对select 'aaa' from table; 的支持
										columnName = String.valueOf(value);
										columnName = columnName.replace("'", "");
										columnName = columnName.replace("\"", "");
										columnName = columnName.replace("`", "");
									}
									columnName = columnName.replace("'", "");
									columnName = columnName.replace("\"", "");
									columnName = columnName.replace("`", "");
									map.put("tableName", tableName);
									map.put("columnName", columnName);
									cols.add(map);
								}
							});
						});
						plainSelect.getWhere().accept(new ExpressionVisitorAdapter() {
							@Override
							public void visit(JdbcNamedParameter jdbcNamedParameter) {
								vars.add(jdbcNamedParameter.getName());
							}
						});
					}
				});
			}
		});
	}

	private void multipleSqlParse(Statement stmt, List<Map<String, String>> cols, List<String> vars) {
		stmt.accept(new StatementVisitorAdapter() {
			@Override
			public void visit(Select select) {
				select.getSelectBody().accept(new SelectVisitorAdapter() {
					@Override
					public void visit(PlainSelect plainSelect) {
						// 存储表名
						Map<String, String> map = new HashMap<>();
						Table table = (Table) plainSelect.getFromItem();
						if (table.getAlias() != null) {
							map.put(table.getName(), table.getAlias().getName());
						}
						for (Join join : plainSelect.getJoins()) {
							Table table1 = (Table) join.getRightItem();
							if (table1.getAlias() != null) {
								map.put(table1.getName(), table1.getAlias().getName());
							}
						}
						plainSelect.getSelectItems().stream().forEach(selectItem -> {
							selectItem.accept(new SelectItemVisitorAdapter() {
								@Override
								public void visit(SelectExpressionItem item) {
									Map<String, String> m = new HashMap<>();
									String tableName = "", columnName;
									SimpleNode node = item.getExpression().getASTNode();
									Object value = node.jjtGetValue();
									if (value instanceof Column) {
										Column column = (Column) value;
										Table table = column.getTable();
										if (table != null) {
											for (Map.Entry<String, String> entry : map.entrySet()) {
												if (table.getName().equals(entry.getValue())) {
													tableName = entry.getKey();
													break;
												}
											}
										}
										columnName = column.getColumnName();
										if (item.getAlias() != null) {
											m.put("columnAliasName", item.getAlias().getName());
										}
									} else if (value instanceof Function) {
										columnName = value.toString();
									} else {
										// 增加对select 'aaa' from table; 的支持
										columnName = String.valueOf(value);
										columnName = columnName.replace("'", "");
										columnName = columnName.replace("\"", "");
										columnName = columnName.replace("`", "");
									}
									columnName = columnName.replace("'", "");
									columnName = columnName.replace("\"", "");
									columnName = columnName.replace("`", "");
									m.put("tableName", tableName);
									m.put("columnName", columnName);
									cols.add(m);
								}
							});
						});
						plainSelect.getWhere().accept(new ExpressionVisitorAdapter() {
							@Override
							public void visit(JdbcNamedParameter jdbcNamedParameter) {
								vars.add(jdbcNamedParameter.getName());
							}
						});
					}
				});
			}
		});
	}

	private String sqlJdbcNamedParameterBuild(DataApiEntity dataApi) throws JSQLParserException {
		Table table = new Table(dataApi.getExecuteConfig().getTableName());
		String[] resParams = dataApi.getResParams().stream().map(s -> s.getFieldName()).toArray(String[]::new);
		Select select = SelectUtils.buildSelectFromTableAndExpressions(table, resParams);
		return SqlBuilderUtil.getInstance().buildHql(select.toString(), dataApi.getReqParams());
	}

	private String sqlJdbcNamedParameterParse(String sqlText) throws JSQLParserException {
		final StringBuilder buffer = new StringBuilder();
		ExpressionDeParser expressionDeParser = new ExpressionDeParser() {
			@Override
			public void visit(JdbcNamedParameter jdbcNamedParameter) {
				this.getBuffer().append("?");
			}
		};
		SelectDeParser deparser = new SelectDeParser(expressionDeParser, buffer);
		expressionDeParser.setSelectVisitor(deparser);
		expressionDeParser.setBuffer(buffer);
		Statement stmt = CCJSqlParserUtil.parse(sqlText);
		stmt.accept(new StatementVisitorAdapter() {
			@Override
			public void visit(Select select) {
				select.getSelectBody().accept(deparser);
			}
		});
		return buffer.toString();
	}

	@Override
	public void copyDataApi(String id) {
		DataApiEntity dataApiEntity = Optional.ofNullable(super.getById(id)).orElseThrow(() -> new DataException("获取失败"));
		DataApiEntity copy = new DataApiEntity();
		copy.setApiName(dataApiEntity.getApiName() + "_副本" + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
		copy.setApiVersion(dataApiEntity.getApiVersion());
		copy.setApiUrl(dataApiEntity.getApiUrl() + "/copy" + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
		copy.setReqMethod(dataApiEntity.getReqMethod());
		copy.setResType(dataApiEntity.getResType());
		copy.setDeny(dataApiEntity.getDeny());
		copy.setRateLimit(dataApiEntity.getRateLimit());
		copy.setExecuteConfig(dataApiEntity.getExecuteConfig());
		copy.setReqParams(dataApiEntity.getReqParams());
		copy.setResParams(dataApiEntity.getResParams());
		copy.setStatus(DataConstant.ApiState.WAIT.getKey());
		dataApiDao.insert(copy);
	}

	@Override
	public void releaseDataApi(String id) {
		Map<String, Object> map = new HashMap<>(2);
		map.put("id", id);
		map.put("type", "1");
		rabbitTemplate.convertAndSend(RabbitMqConstant.FANOUT_EXCHANGE_API, "", map);
		LambdaUpdateWrapper<DataApiEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.set(DataApiEntity::getStatus, DataConstant.ApiState.RELEASE.getKey());
		updateWrapper.eq(DataApiEntity::getId, id);
		dataApiDao.update(null, updateWrapper);
	}

	@Override
	public void cancelDataApi(String id) {
		Map<String, Object> map = new HashMap<>(2);
		map.put("id", id);
		map.put("type", "2");
		rabbitTemplate.convertAndSend(RabbitMqConstant.FANOUT_EXCHANGE_API, "", map);
		LambdaUpdateWrapper<DataApiEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.set(DataApiEntity::getStatus, DataConstant.ApiState.CANCEL.getKey());
		updateWrapper.eq(DataApiEntity::getId, id);
		dataApiDao.update(null, updateWrapper);
	}

	@Override
	public Map<String, Object> getDataApiDetailById(String id) {
		DataApiEntity dataApiEntity = super.getById(id);
		ApiHeader apiHeader = new ApiHeader();
		MD5Util mt = null;
		try {
			mt = MD5Util.getInstance();
			apiHeader.setApiKey(mt.encode(id));
			apiHeader.setSecretKey(mt.encode(SecurityUtil.getUserId()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, Object> map = new HashMap<>(2);
		map.put("data", dataApiMapper.toVO(dataApiEntity));
		map.put("header", apiHeader);
		return map;
	}

	@Override
	public DataApiEntity getBySourceId(String sourceId) {
		return dataApiDao.selectOne(new QueryWrapper<DataApiEntity>().eq("source_id", sourceId).last("limit 1"));
	}
}
