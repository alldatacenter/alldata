package cn.datax.service.data.standard.service.impl;

import cn.datax.common.base.BaseServiceImpl;
import cn.datax.service.data.standard.api.dto.ContrastDto;
import cn.datax.service.data.standard.api.entity.ContrastDictEntity;
import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.vo.ContrastTreeVo;
import cn.datax.service.data.standard.dao.ContrastDao;
import cn.datax.service.data.standard.dao.ContrastDictDao;
import cn.datax.service.data.standard.mapstruct.ContrastMapper;
import cn.datax.service.data.standard.service.ContrastService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 对照表信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ContrastServiceImpl extends BaseServiceImpl<ContrastDao, ContrastEntity> implements ContrastService {

	@Autowired
	private ContrastDao contrastDao;

	@Autowired
	private ContrastDictDao contrastDictDao;

	@Autowired
	private ContrastMapper contrastMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContrastEntity saveContrast(ContrastDto contrastDto) {
		ContrastEntity contrast = contrastMapper.toEntity(contrastDto);
		contrastDao.insert(contrast);
		return contrast;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContrastEntity updateContrast(ContrastDto contrastDto) {
		ContrastEntity contrast = contrastMapper.toEntity(contrastDto);
		contrastDao.updateById(contrast);
		return contrast;
	}

	@Override
	public ContrastEntity getContrastById(String id) {
		ContrastEntity contrastEntity = super.getById(id);
		return contrastEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteContrastById(String id) {
		ContrastDictEntity dictEntity = contrastDictDao.selectOne(new QueryWrapper<ContrastDictEntity>().eq("contrast_id", id).last("limit 1"));
		if (dictEntity != null) {
			throw new RuntimeException("存在对照字典数据与之关联，不允许删除！");
		}
		contrastDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteContrastBatch(List<String> ids) {
		for (String id : ids) {
			deleteContrastById(id);
		}
		//contrastDao.deleteBatchIds(ids);
	}

	@Override
	public List<ContrastTreeVo> getContrastTree() {
		List<ContrastTreeVo> list = new ArrayList<>();
		List<ContrastEntity> contrastEntityList = contrastDao.selectList(Wrappers.emptyWrapper());
		Map<String, List<ContrastEntity>> sourceMap = contrastEntityList.stream().collect(Collectors.groupingBy(ContrastEntity::getSourceId));
		Iterator<Map.Entry<String, List<ContrastEntity>>> sourceIterator = sourceMap.entrySet().iterator();
		while (sourceIterator.hasNext()) {
			Map.Entry<String, List<ContrastEntity>> sourceEntry = sourceIterator.next();
			String sourceId = sourceEntry.getKey();
			List<ContrastEntity> sourceList = sourceEntry.getValue();
			String sourceName = sourceList.get(0).getSourceName();
			ContrastTreeVo sourceTree = new ContrastTreeVo();
			sourceTree.setId(sourceId);
			sourceTree.setLabel(sourceName);
			Map<String, List<ContrastEntity>> tableMap = sourceList.stream().collect(Collectors.groupingBy(ContrastEntity::getTableId));
			Iterator<Map.Entry<String, List<ContrastEntity>>> tableIterator = tableMap.entrySet().iterator();
			List<ContrastTreeVo> tableTreeList = new ArrayList<>();
			while (tableIterator.hasNext()) {
				Map.Entry<String, List<ContrastEntity>> tableEntry = tableIterator.next();
				String tableId = tableEntry.getKey();
				List<ContrastEntity> tableList = tableEntry.getValue();
				String tableName = tableList.get(0).getTableName();
				String tableComment = tableList.get(0).getTableComment();
				ContrastTreeVo tableTree = new ContrastTreeVo();
				tableTree.setId(tableId);
				tableTree.setLabel(tableName);
				tableTree.setName(tableComment);
				List<ContrastTreeVo> columnTreeList = tableList.stream().map(s -> {
					ContrastTreeVo columnTree = new ContrastTreeVo();
					columnTree.setId(s.getId());
					columnTree.setLabel(s.getColumnName());
					columnTree.setName(s.getColumnComment());
					columnTree.setData(s);
					return columnTree;
				}).collect(Collectors.toList());
				tableTree.setChildren(columnTreeList);
				tableTreeList.add(tableTree);
			}
			sourceTree.setChildren(tableTreeList);
			list.add(sourceTree);
		}
		return list;
	}

	@Override
	public IPage<ContrastEntity> statistic(IPage<ContrastEntity> page, Wrapper<ContrastEntity> queryWrapper) {
		return contrastDao.statistic(page, queryWrapper);
	}

	@Override
	public ContrastEntity getBySourceId(String sourceId) {
		return contrastDao.selectOne(new QueryWrapper<ContrastEntity>().eq("source_id", sourceId).last("limit 1"));
	}
}
