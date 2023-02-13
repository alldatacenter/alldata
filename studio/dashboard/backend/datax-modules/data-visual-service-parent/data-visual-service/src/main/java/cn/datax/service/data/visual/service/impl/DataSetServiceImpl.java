package cn.datax.service.data.visual.service.impl;

import cn.datax.common.exception.DataException;
import cn.datax.common.utils.ThrowableUtil;
import cn.datax.service.data.visual.api.dto.SqlParseDto;
import cn.datax.service.data.visual.api.entity.ChartEntity;
import cn.datax.service.data.visual.api.entity.DataSetEntity;
import cn.datax.service.data.visual.api.dto.DataSetDto;
import cn.datax.service.data.visual.dao.ChartDao;
import cn.datax.service.data.visual.service.DataSetService;
import cn.datax.service.data.visual.mapstruct.DataSetMapper;
import cn.datax.service.data.visual.dao.DataSetDao;
import cn.datax.common.base.BaseServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.netflix.discovery.converters.Auto;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 数据集信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DataSetServiceImpl extends BaseServiceImpl<DataSetDao, DataSetEntity> implements DataSetService {

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private ChartDao chartDao;

	@Autowired
	private DataSetMapper dataSetMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public DataSetEntity saveDataSet(DataSetDto dataSetDto) {
		DataSetEntity dataSet = dataSetMapper.toEntity(dataSetDto);
		dataSetDao.insert(dataSet);
		return dataSet;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public DataSetEntity updateDataSet(DataSetDto dataSetDto) {
		DataSetEntity dataSet = dataSetMapper.toEntity(dataSetDto);
		dataSetDao.updateById(dataSet);
		return dataSet;
	}

	@Override
	public DataSetEntity getDataSetById(String id) {
		DataSetEntity dataSetEntity = super.getById(id);
		return dataSetEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteDataSetById(String id) {
		//zrx 查看是否有与之关联的图表
		ChartEntity chartEntity = chartDao.selectOne(new QueryWrapper<ChartEntity>().like("chart_json", id));
		if (chartEntity != null) {
			throw new RuntimeException("有与之关联的图表，不允许删除！");
		}
		dataSetDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteDataSetBatch(List<String> ids) {
		//zrx
		for (String id : ids) {
			deleteDataSetById(id);
		}
		//dataSetDao.deleteBatchIds(ids);
	}

	@Override
	public List<String> sqlAnalyse(SqlParseDto sqlParseDto) {
		String sql = sqlParseDto.getSqlText();
		Statement stmt;
		try {
			stmt = CCJSqlParserUtil.parse(sql);
		} catch (JSQLParserException e) {
			log.error("全局异常信息ex={}, StackTrace={}", e.getMessage(), ThrowableUtil.getStackTrace(e));
			throw new DataException("SQL语法有问题，解析出错");
		}
		List<String> cols = new ArrayList<>();
		if (stmt instanceof Select) {
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
										String columnName;
										if (item.getAlias() == null) {
											SimpleNode node = item.getExpression().getASTNode();
											Object value = node.jjtGetValue();
											if (value instanceof Column) {
												columnName = ((Column) value).getColumnName();
											} else if (value instanceof Function) {
												columnName = value.toString();
											} else {
												// 增加对select 'aaa' from table; 的支持
												columnName = String.valueOf(value);
												columnName = columnName.replace("'", "");
												columnName = columnName.replace("\"", "");
												columnName = columnName.replace("`", "");
											}
										} else {
											columnName = item.getAlias().getName();
										}
										columnName = columnName.replace("'", "");
										columnName = columnName.replace("\"", "");
										columnName = columnName.replace("`", "");
										cols.add(columnName);
									}
								});
							});
						}
					});
				}
			});
		}
		return cols;
	}

	@Override
	public DataSetEntity getBySourceId(String sourceId) {
		return dataSetDao.selectOne(new QueryWrapper<DataSetEntity>().eq("source_id", sourceId).last("limit 1"));
	}
}
