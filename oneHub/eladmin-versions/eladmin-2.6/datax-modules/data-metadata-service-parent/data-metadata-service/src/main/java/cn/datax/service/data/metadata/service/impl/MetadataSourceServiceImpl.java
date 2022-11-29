package cn.datax.service.data.metadata.service.impl;

import cn.datax.commo.office.word.WordUtil;
import cn.datax.common.base.BaseServiceImpl;
import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.database.DataSourceFactory;
import cn.datax.common.database.DbQuery;
import cn.datax.common.database.constants.DbQueryProperty;
import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.feign.DataApiServiceFeign;
import cn.datax.service.data.metadata.api.dto.DbSchema;
import cn.datax.service.data.metadata.api.dto.MetadataSourceDto;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.api.entity.MetadataChangeRecordEntity;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.enums.DataLevel;
import cn.datax.service.data.metadata.api.enums.SyncStatus;
import cn.datax.service.data.metadata.async.AsyncTask;
import cn.datax.service.data.metadata.dao.MetadataAuthorizeDao;
import cn.datax.service.data.metadata.dao.MetadataChangeRecordDao;
import cn.datax.service.data.metadata.dao.MetadataColumnDao;
import cn.datax.service.data.metadata.dao.MetadataSourceDao;
import cn.datax.service.data.metadata.dao.MetadataTableDao;
import cn.datax.service.data.metadata.mapstruct.MetadataSourceMapper;
import cn.datax.service.data.metadata.service.MetadataSourceService;
import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import cn.datax.service.data.quality.api.feign.QualityServiceFeign;
import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.feign.StandardServiceFeign;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.feign.VisualServiceFeign;
import cn.hutool.core.util.StrUtil;
import com.aspose.words.Document;
import com.aspose.words.MailMerge;
import com.aspose.words.net.System.Data.DataRelation;
import com.aspose.words.net.System.Data.DataRow;
import com.aspose.words.net.System.Data.DataSet;
import com.aspose.words.net.System.Data.DataTable;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 数据源信息表 服务实现类
 * </p>
 *
 * @author yuwei
 * @since 2020-03-14
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MetadataSourceServiceImpl extends BaseServiceImpl<MetadataSourceDao, MetadataSourceEntity> implements MetadataSourceService {

	@Autowired
	private MetadataSourceDao metadataSourceDao;

	@Autowired
	private MetadataSourceMapper metadataSourceMapper;

	@Autowired
	private DataSourceFactory dataSourceFactory;

	@Autowired
	private AsyncTask asyncTask;

	@Autowired
	private MetadataTableDao metadataTableDao;

	@Autowired
	private MetadataColumnDao metadataColumnDao;

	@Autowired
	private MetadataAuthorizeDao metadataAuthorizeDao;

	@Autowired
	private MetadataChangeRecordDao metadataChangeRecordDao;

	@Autowired
	private StandardServiceFeign standardServiceFeign;

	@Autowired
	private QualityServiceFeign qualityServiceFeign;

	@Autowired
	private DataApiServiceFeign dataApiServiceFeign;

	@Autowired
	private VisualServiceFeign visualServiceFeign;

	@Autowired
	private RedisService redisService;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveMetadataSource(MetadataSourceDto metadataSourceDto) {
		MetadataSourceEntity dataSource = metadataSourceMapper.toEntity(metadataSourceDto);
		dataSource.setIsSync(SyncStatus.NotSync.getKey());
		metadataSourceDao.insert(dataSource);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateMetadataSource(MetadataSourceDto metadataSourceDto) {
		MetadataSourceEntity dataSource = metadataSourceMapper.toEntity(metadataSourceDto);
		metadataSourceDao.updateById(dataSource);
	}

	@Override
	public MetadataSourceEntity getMetadataSourceById(String id) {
		MetadataSourceEntity metadataSourceEntity = super.getById(id);
		return metadataSourceEntity;
	}

	@Override
	public List<MetadataSourceEntity> getMetadataSourceList() {
		boolean admin = SecurityUtil.isAdmin();
		List<MetadataSourceEntity> sourceList = (List<MetadataSourceEntity>) redisService.get(RedisConstant.METADATA_SOURCE_KEY);
		Stream<MetadataSourceEntity> stream = Optional.ofNullable(sourceList).orElseGet(ArrayList::new).stream()
				.filter(s -> DataConstant.EnableState.ENABLE.getKey().equals(s.getStatus()));
		if (!admin) {
			Set<String> set = new HashSet<>();
			List<String> roleIds = SecurityUtil.getUserRoleIds();
			roleIds.stream().forEach(role -> {
				List<MetadataAuthorizeEntity> list = (List<MetadataAuthorizeEntity>) redisService.hget(RedisConstant.METADATA_AUTHORIZE_KEY, role);
				set.addAll(Optional.ofNullable(list).orElseGet(ArrayList::new).stream()
						.filter(s -> Objects.equals(DataLevel.DATABASE.getKey(), s.getObjectType()))
						.map(s -> s.getObjectId()).collect(Collectors.toSet()));
			});
			stream = stream.filter(s -> set.contains(s.getId()));
		}
		return stream.collect(Collectors.toList());
	}

	@Override
	public <E extends IPage<MetadataSourceEntity>> E pageWithAuth(E page, Wrapper<MetadataSourceEntity> queryWrapper) {
		boolean admin = SecurityUtil.isAdmin();
		List<String> roles = new ArrayList<>();
		if (!admin) {
			roles = SecurityUtil.getUserRoleIds();
		}
		return metadataSourceDao.selectPageWithAuth(page, queryWrapper, roles);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteMetadataSourceById(String id) {
		checkSourceRel(id);
		deleteTableAndCol(id);
		metadataSourceDao.deleteById(id);
	}

	/**
	 * 检查与数据源的依赖
	 *
	 * @param id
	 */
	private void checkSourceRel(String id) {
		//查询授权关联
		MetadataAuthorizeEntity metadataAuthorizeEntity = metadataAuthorizeDao.selectOne(new QueryWrapper<MetadataAuthorizeEntity>().eq("object_id", id).eq("object_type", "database").last("limit 1"));
		if (metadataAuthorizeEntity != null) {
			throw new RuntimeException("元数据管理-数据授权有与之关联的数据，不允许删除！");
		}
		//查询变更记录关联
		MetadataChangeRecordEntity recordEntity = metadataChangeRecordDao.selectRelColumnBySourceId(id);
		if (recordEntity != null) {
			throw new RuntimeException("元数据管理-数据变更记录有与之关联的数据，不允许删除！");
		}
		//对照表关联
		ContrastEntity contrastEntity = standardServiceFeign.getBySourceId(id);
		if (contrastEntity != null) {
			throw new RuntimeException("数据标准管理-对照表有与之关联的数据，不允许删除！");
		}
		//数据质量关联
		CheckRuleEntity checkRuleEntity = qualityServiceFeign.getBySourceId(id);
		if (checkRuleEntity != null) {
			throw new RuntimeException("数据质量管理-规则配置有与之关联的数据，不允许删除！");
		}
		//数据集市关联
		DataApiEntity dataApiEntity = dataApiServiceFeign.getBySourceId(id);
		if (dataApiEntity != null) {
			throw new RuntimeException("数据集市管理-数据服务有与之关联的数据，不允许删除！");
		}
		//数据可视化关联
		DataSetEntity dataSetEntity = visualServiceFeign.getBySourceId(id);
		if (dataSetEntity != null) {
			throw new RuntimeException("可视化管理-数据集有与之关联的数据，不允许删除！");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteTableAndCol(String id) {
		//同步删除表和字段
		metadataTableDao.delete(new QueryWrapper<MetadataTableEntity>().eq("source_id", id));
		metadataColumnDao.delete(new QueryWrapper<MetadataColumnEntity>().eq("source_id", id));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteMetadataSourceBatch(List<String> ids) {
		for (String id : ids) {
			deleteMetadataSourceById(id);
		}
		//metadataSourceDao.deleteBatchIds(ids);
	}

	@Override
	public DbQuery checkConnection(MetadataSourceDto metadataSourceDto) {
		MetadataSourceEntity dataSource = metadataSourceMapper.toEntity(metadataSourceDto);
		DbSchema dbSchema = dataSource.getDbSchema();
		DbQueryProperty dbQueryProperty = new DbQueryProperty(dataSource.getDbType(), dbSchema.getHost(),
				dbSchema.getUsername(), dbSchema.getPassword(), dbSchema.getPort(), dbSchema.getDbName(), dbSchema.getSid());
		DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
		return dbQuery;
	}

	@Override
	public DbQuery getDbQuery(String id) {
		MetadataSourceEntity dataSource = super.getById(id);
		DbSchema dbSchema = dataSource.getDbSchema();
		DbQueryProperty dbQueryProperty = new DbQueryProperty(dataSource.getDbType(), dbSchema.getHost(),
				dbSchema.getUsername(), dbSchema.getPassword(), dbSchema.getPort(), dbSchema.getDbName(), dbSchema.getSid());
		DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
		return dbQuery;
	}

	@Override
	public List<DbTable> getDbTables(String id) {
		MetadataSourceEntity dataSource = super.getById(id);
		DbSchema dbSchema = dataSource.getDbSchema();
		DbQueryProperty dbQueryProperty = new DbQueryProperty(dataSource.getDbType(), dbSchema.getHost(),
				dbSchema.getUsername(), dbSchema.getPassword(), dbSchema.getPort(), dbSchema.getDbName(), dbSchema.getSid());
		DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
		List<DbTable> tables = dbQuery.getTables(dbSchema.getDbName());
		return tables;
	}

	@Override
	public List<DbColumn> getDbTableColumns(String id, String tableName) {
		MetadataSourceEntity dataSource = super.getById(id);
		DbSchema dbSchema = dataSource.getDbSchema();
		DbQueryProperty dbQueryProperty = new DbQueryProperty(dataSource.getDbType(), dbSchema.getHost(),
				dbSchema.getUsername(), dbSchema.getPassword(), dbSchema.getPort(), dbSchema.getDbName(), dbSchema.getSid());
		DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
		List<DbColumn> columns = dbQuery.getTableColumns(dbSchema.getDbName(), tableName);
		return columns;
	}

	@Override
	public void syncMetadata(String id) {
		MetadataSourceEntity metadataSourceEntity = super.getById(id);
		if (SyncStatus.InSync.getKey().equals(metadataSourceEntity.getIsSync())) {
			throw new DataException("元数据同步中");
		}
		// 异步执行同步任务
		asyncTask.doTask(metadataSourceEntity, this);
	}

	@Override
	public Document wordMetadata(String id) throws Exception {
		MetadataSourceEntity metadataSourceEntity = super.getById(id);
		DbSchema dbSchema = metadataSourceEntity.getDbSchema();
		String dbName = dbSchema.getDbName();
		if (StrUtil.isBlank(dbName)) {
			dbName = dbSchema.getUsername();
		}
		QueryWrapper<MetadataTableEntity> tableQueryWrapper = new QueryWrapper<>();
		tableQueryWrapper.eq("source_id", id);
		List<MetadataTableEntity> tableEntityList = metadataTableDao.selectList(tableQueryWrapper);
		// 数据表（主表） TableStart:TableList TableEnd:TableList
		DataTable tableTable = new DataTable("TableList");
		tableTable.getColumns().add("id");
		tableTable.getColumns().add("tableName");
		tableTable.getColumns().add("tableComment");
		for (MetadataTableEntity metadataTableEntity : tableEntityList) {
			DataRow row = tableTable.newRow();
			row.set(0, metadataTableEntity.getId());
			row.set(1, metadataTableEntity.getTableName());
			row.set(2, metadataTableEntity.getTableComment());
			tableTable.getRows().add(row);
		}
		QueryWrapper<MetadataColumnEntity> columnQueryWrapper = new QueryWrapper<>();
		columnQueryWrapper.eq("source_id", id);
		columnQueryWrapper.orderByAsc("column_position");
		List<MetadataColumnEntity> columnEntityList = metadataColumnDao.selectList(columnQueryWrapper);
		// 元数据（子表） TableStart:ColumnList TableEnd:ColumnList
		DataTable columnTable = new DataTable("ColumnList");
		columnTable.getColumns().add("id");
		columnTable.getColumns().add("tid");
		columnTable.getColumns().add("columnPosition");
		columnTable.getColumns().add("columnName");
		columnTable.getColumns().add("dataType");
		columnTable.getColumns().add("dataLength");
		columnTable.getColumns().add("dataPrecision");
		columnTable.getColumns().add("dataScale");
		columnTable.getColumns().add("columnNullable");
		columnTable.getColumns().add("columnKey");
		columnTable.getColumns().add("dataDefault");
		columnTable.getColumns().add("columnComment");
		for (MetadataColumnEntity metadataColumnEntity : columnEntityList) {
			DataRow row = columnTable.newRow();
			row.set(0, metadataColumnEntity.getId());
			row.set(1, metadataColumnEntity.getTableId());
			row.set(2, metadataColumnEntity.getColumnPosition());
			row.set(3, metadataColumnEntity.getColumnName());
			row.set(4, metadataColumnEntity.getDataType());
			row.set(5, metadataColumnEntity.getDataLength());
			row.set(6, metadataColumnEntity.getDataPrecision());
			row.set(7, metadataColumnEntity.getDataScale());
			row.set(8, "1".equals(metadataColumnEntity.getColumnNullable()) ? "Y" : "N");
			row.set(9, "1".equals(metadataColumnEntity.getColumnKey()) ? "Y" : "N");
			row.set(10, metadataColumnEntity.getDataDefault());
			row.set(11, metadataColumnEntity.getColumnComment());
			columnTable.getRows().add(row);
		}
		// 提供数据源
		DataSet dataSet = new DataSet();
		dataSet.getTables().add(tableTable);
		dataSet.getTables().add(columnTable);
		DataRelation dataRelation = new DataRelation("TableColumnRelation", tableTable.getColumns().get("id"), columnTable.getColumns().get("tid"));
		dataSet.getRelations().add(dataRelation);
		// 合并模版
		ClassPathResource classPathResource = new ClassPathResource("templates/metadata_1.0.0.doc");
		InputStream inputStream = classPathResource.getInputStream();
		Document doc = WordUtil.getInstance().getDocument(inputStream);
		// 提供数据源
		String[] fieldNames = new String[]{"database"};
		Object[] fieldValues = new Object[]{dbName.toUpperCase()};
		MailMerge mailMerge = doc.getMailMerge();
		mailMerge.execute(fieldNames, fieldValues);
		mailMerge.executeWithRegions(dataSet);
		WordUtil.getInstance().insertWatermarkText(doc, SecurityUtil.getUserName());
		return doc;
	}

	@Override
	@Async("taskExecutor")
	public void refreshMetadata() {
		String sourceKey = RedisConstant.METADATA_SOURCE_KEY;
		Boolean hasSourceKey = redisService.hasKey(sourceKey);
		if (hasSourceKey) {
			redisService.del(sourceKey);
		}
		List<MetadataSourceEntity> sourceEntityList = metadataSourceDao.selectList(Wrappers.emptyWrapper());
		redisService.set(sourceKey, sourceEntityList);

		String tableKey = RedisConstant.METADATA_TABLE_KEY;
		Boolean hasTableKey = redisService.hasKey(tableKey);
		if (hasTableKey) {
			redisService.del(tableKey);
		}
		List<MetadataTableEntity> tableEntityList = metadataTableDao.selectList(Wrappers.emptyWrapper());
		Map<String, List<MetadataTableEntity>> tableListMap = tableEntityList.stream().collect(Collectors.groupingBy(MetadataTableEntity::getSourceId));
		redisTemplate.opsForHash().putAll(tableKey, tableListMap);

		String columnKey = RedisConstant.METADATA_COLUMN_KEY;
		Boolean hasColumnKey = redisService.hasKey(columnKey);
		if (hasColumnKey) {
			redisService.del(columnKey);
		}
		List<MetadataColumnEntity> columnEntityList = metadataColumnDao.selectList(Wrappers.emptyWrapper());
		Map<String, List<MetadataColumnEntity>> columnListMap = columnEntityList.stream().collect(Collectors.groupingBy(MetadataColumnEntity::getTableId));
		redisTemplate.opsForHash().putAll(columnKey, columnListMap);
	}
}
