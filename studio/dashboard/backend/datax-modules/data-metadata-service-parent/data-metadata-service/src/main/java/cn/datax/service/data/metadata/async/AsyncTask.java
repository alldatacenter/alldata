package cn.datax.service.data.metadata.async;

import cn.datax.common.database.DataSourceFactory;
import cn.datax.common.database.DbQuery;
import cn.datax.common.database.constants.DbQueryProperty;
import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import cn.datax.service.data.metadata.api.dto.DbSchema;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.enums.SyncStatus;
import cn.datax.service.data.metadata.dao.MetadataColumnDao;
import cn.datax.service.data.metadata.dao.MetadataSourceDao;
import cn.datax.service.data.metadata.dao.MetadataTableDao;
import cn.datax.service.data.metadata.service.MetadataSourceService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 异步处理
 */
@Slf4j
@Component
public class AsyncTask {

	@Autowired
	private DataSourceFactory dataSourceFactory;

	@Autowired
	private MetadataSourceDao metadataSourceDao;

	@Autowired
	private MetadataTableDao metadataTableDao;

	@Autowired
	private MetadataColumnDao metadataColumnDao;

	@Async("taskExecutor")
	public void doTask(MetadataSourceEntity dataSource, MetadataSourceService metadataSourceService) {
		dataSource.setIsSync(SyncStatus.InSync.getKey());
		metadataSourceDao.updateById(dataSource);
		long start = System.currentTimeMillis();
		try {
			DbSchema dbSchema = dataSource.getDbSchema();
			DbQueryProperty dbQueryProperty = new DbQueryProperty(dataSource.getDbType(), dbSchema.getHost(),
					dbSchema.getUsername(), dbSchema.getPassword(), dbSchema.getPort(), dbSchema.getDbName(), dbSchema.getSid());
			DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
			List<DbTable> tables = dbQuery.getTables(dbSchema.getDbName());
			if (CollUtil.isNotEmpty(tables)) {
				List<MetadataTableEntity> metadataTableEntityList = tables.stream().map(table -> {
					MetadataTableEntity metadataTable = new MetadataTableEntity();
					metadataTable.setSourceId(dataSource.getId());
					metadataTable.setTableName(table.getTableName());
					metadataTable.setTableComment(table.getTableComment());
					return metadataTable;
				}).collect(Collectors.toList());
				if (CollUtil.isNotEmpty(metadataTableEntityList)) {
					for (MetadataTableEntity table : metadataTableEntityList) {
						MetadataTableEntity tableEntity = metadataTableDao.selectOne(new QueryWrapper<MetadataTableEntity>()
								.eq("source_id", dataSource.getId())
								.eq("table_name", table.getTableName())
								.last("limit 1"));
						if (tableEntity != null) {
							table.setId(tableEntity.getId());
							metadataTableDao.updateById(table);
						} else {
							metadataTableDao.insert(table);
						}
						List<DbColumn> columns = dbQuery.getTableColumns(dbSchema.getDbName(), table.getTableName());
						if (CollUtil.isNotEmpty(columns)) {
							List<MetadataColumnEntity> metadataColumnEntityList = columns.stream().map(column -> {
								MetadataColumnEntity metadataColumn = new MetadataColumnEntity();
								metadataColumn.setSourceId(dataSource.getId());
								metadataColumn.setTableId(table.getId());
								metadataColumn.setColumnName(column.getColName());
								metadataColumn.setColumnComment(column.getColComment());
								metadataColumn.setColumnKey(column.getColKey() ? "1" : "0");
								metadataColumn.setColumnNullable(column.getNullable() ? "1" : "0");
								metadataColumn.setColumnPosition(column.getColPosition().toString());
								metadataColumn.setDataType(column.getDataType());
								metadataColumn.setDataLength(column.getDataLength());
								metadataColumn.setDataPrecision(column.getDataPrecision());
								metadataColumn.setDataScale(column.getDataScale());
								metadataColumn.setDataDefault(column.getDataDefault());
								return metadataColumn;
							}).collect(Collectors.toList());
							if (CollUtil.isNotEmpty(metadataColumnEntityList)) {
								for (MetadataColumnEntity column : metadataColumnEntityList) {
									MetadataColumnEntity columnEntity = metadataColumnDao.selectOne(new QueryWrapper<MetadataColumnEntity>()
											.eq("table_id", table.getId())
											.eq("column_name", column.getColumnName())
											.last("limit 1"));
									if (columnEntity != null) {
										column.setId(columnEntity.getId());
										metadataColumnDao.updateById(column);
									} else {
										metadataColumnDao.insert(column);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			dataSource.setIsSync(SyncStatus.SyncError.getKey());
			metadataSourceDao.updateById(dataSource);
			log.error("异步数据源 {} 同步出错！", dataSource.getSourceName(), e);
			return;
		}
		dataSource.setIsSync(SyncStatus.IsSync.getKey());
		metadataSourceDao.updateById(dataSource);
		metadataSourceService.refreshMetadata();
		log.info("异步数据源 {} 同步任务执行完成！耗时{}秒", dataSource.getSourceName(), (System.currentTimeMillis() - start / 1000));
	}
}
