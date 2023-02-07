package cn.datax.service.workflow.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.service.workflow.api.entity.BusinessEntity;
import cn.datax.service.workflow.api.dto.BusinessDto;
import cn.datax.service.workflow.service.BusinessService;
import cn.datax.service.workflow.mapstruct.BusinessMapper;
import cn.datax.service.workflow.dao.BusinessDao;
import cn.datax.common.base.BaseServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 业务流程配置表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-22
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class BusinessServiceImpl extends BaseServiceImpl<BusinessDao, BusinessEntity> implements BusinessService {

	@Autowired
	private BusinessDao businessDao;

	@Autowired
	private BusinessMapper businessMapper;

	@Autowired
	private RedisService redisService;

	@Autowired
	private RuntimeService runtimeService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BusinessEntity saveBusiness(BusinessDto businessDto) {
		BusinessEntity business = businessMapper.toEntity(businessDto);
		int n = businessDao.selectCount(Wrappers.<BusinessEntity>lambdaQuery().eq(BusinessEntity::getBusinessCode, business.getBusinessCode()));
		if (n > 0) {
			throw new DataException("该业务编码已存在");
		}
		businessDao.insert(business);
		return business;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BusinessEntity updateBusiness(BusinessDto businessDto) {
		BusinessEntity business = businessMapper.toEntity(businessDto);
		businessDao.updateById(business);
		return business;
	}

	@Override
	public BusinessEntity getBusinessById(String id) {
		BusinessEntity businessEntity = super.getById(id);
		return businessEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBusinessById(String id) {
		// zrx check
		ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();
		List<ProcessInstance> processInstances = processInstanceQuery.includeProcessVariables()
				.orderByStartTime().desc()
				.listPage(0, 1);
		if (!processInstances.isEmpty()) {
			throw new RuntimeException("存在运行中的流程，不允许删除！");
		}
		businessDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBusinessBatch(List<String> ids) {
		// zrx add
		for (String id : ids) {
			deleteBusinessById(id);
		}
		//businessDao.deleteBatchIds(ids);
	}

	@Override
	public void refreshBusiness() {
		String key = RedisConstant.WORKFLOW_BUSINESS_KEY;
		Boolean hasKey = redisService.hasKey(key);
		if (hasKey) {
			redisService.del(key);
		}
		List<BusinessEntity> businessEntityList = businessDao.selectList(Wrappers.<BusinessEntity>lambdaQuery()
				.eq(BusinessEntity::getStatus, DataConstant.EnableState.ENABLE.getKey()));
		// 第一个参数BusinessEntity::getBusinessCode 表示选择BusinessEntity的businessCode作为map的key值
		// 第二个参数v -> v表示选择将原来的对象作为map的value值
		// 第三个参数(v1, v2) -> v2中，如果v1与v2的key值相同，选择v2作为那个key所对应的value值
		Map<String, Object> map = businessEntityList.stream().collect(Collectors.toMap(BusinessEntity::getBusinessCode, v -> v, (v1, v2) -> v2));
		redisService.hmset(key, map);
	}

	@Override
	public void checkHasDefId(String id) {
		BusinessEntity businessEntity = businessDao.selectOne(new QueryWrapper<BusinessEntity>().eq("process_definition_id", id).last("limit 1"));
		if (businessEntity != null) {
			throw new RuntimeException("存在与之关联的业务配置，不允许删除！");
		}
	}
}
