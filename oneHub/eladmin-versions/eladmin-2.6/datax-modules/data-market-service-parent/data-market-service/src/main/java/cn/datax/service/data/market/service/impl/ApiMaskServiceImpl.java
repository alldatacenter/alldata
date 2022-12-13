package cn.datax.service.data.market.service.impl;

import cn.datax.common.base.BaseServiceImpl;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.service.data.market.api.dto.ApiMaskDto;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.dao.ApiMaskDao;
import cn.datax.service.data.market.mapstruct.ApiMaskMapper;
import cn.datax.service.data.market.service.ApiMaskService;
import cn.datax.service.system.api.entity.DeptEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 数据API脱敏信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApiMaskServiceImpl extends BaseServiceImpl<ApiMaskDao, ApiMaskEntity> implements ApiMaskService {

    @Autowired
    private ApiMaskDao apiMaskDao;

    @Autowired
    private ApiMaskMapper apiMaskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveApiMask(ApiMaskDto apiMaskDto) {
        ApiMaskEntity apiMask = apiMaskMapper.toEntity(apiMaskDto);
        // 校验api唯一
        int n = apiMaskDao.selectCount(Wrappers.<ApiMaskEntity>lambdaQuery().eq(ApiMaskEntity::getApiId, apiMask.getApiId()));
        if(n > 0){
            throw new DataException("该api已进行过脱敏配置");
        }
        apiMaskDao.insert(apiMask);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApiMask(ApiMaskDto apiMaskDto) {
        ApiMaskEntity apiMask = apiMaskMapper.toEntity(apiMaskDto);
        apiMaskDao.updateById(apiMask);
    }

    @Override
    public ApiMaskEntity getApiMaskById(String id) {
        ApiMaskEntity apiMaskEntity = super.getById(id);
        return apiMaskEntity;
    }

    @Override
    public ApiMaskEntity getApiMaskByApiId(String apiId) {
        ApiMaskEntity apiMaskEntity = apiMaskDao.selectOne(new QueryWrapper<ApiMaskEntity>().eq("api_id", apiId));
        return apiMaskEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiMaskById(String id) {
        apiMaskDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiMaskBatch(List<String> ids) {
        apiMaskDao.deleteBatchIds(ids);
    }
}
