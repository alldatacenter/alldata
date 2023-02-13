package cn.datax.service.data.standard.service.impl;

import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.entity.DictEntity;
import cn.datax.service.data.standard.api.entity.TypeEntity;
import cn.datax.service.data.standard.api.dto.TypeDto;
import cn.datax.service.data.standard.dao.ContrastDao;
import cn.datax.service.data.standard.dao.ContrastDictDao;
import cn.datax.service.data.standard.dao.DictDao;
import cn.datax.service.data.standard.service.TypeService;
import cn.datax.service.data.standard.mapstruct.TypeMapper;
import cn.datax.service.data.standard.dao.TypeDao;
import cn.datax.common.base.BaseServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 数据标准类别表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class TypeServiceImpl extends BaseServiceImpl<TypeDao, TypeEntity> implements TypeService {

	@Autowired
	private TypeDao typeDao;

	@Autowired
	private DictDao dictDao;

	@Autowired
	private ContrastDao contrastDao;

	@Autowired
	private TypeMapper typeMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TypeEntity saveType(TypeDto typeDto) {
		TypeEntity type = typeMapper.toEntity(typeDto);
		typeDao.insert(type);
		return type;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TypeEntity updateType(TypeDto typeDto) {
		TypeEntity type = typeMapper.toEntity(typeDto);
		typeDao.updateById(type);
		return type;
	}

	@Override
	public TypeEntity getTypeById(String id) {
		TypeEntity typeEntity = super.getById(id);
		return typeEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteTypeById(String id) {
		//zrx 查询是否有字典
		DictEntity dictEntity = dictDao.selectOne(new QueryWrapper<DictEntity>().eq("type_id", id).last("limit 1"));
		if (dictEntity != null) {
			throw new RuntimeException("该类别下有标准字典与之关联，不允许删除！");
		}
		ContrastEntity contrastEntity = contrastDao.selectOne(new QueryWrapper<ContrastEntity>().eq("gb_type_id", id).last("limit 1"));
		if (contrastEntity != null) {
			throw new RuntimeException("该类别有对照表与之关联，不允许删除！");
		}
		typeDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteTypeBatch(List<String> ids) {
		// zrx add
		for (String id : ids) {
			deleteTypeById(id);
		}
		// typeDao.deleteBatchIds(ids);
	}
}
