package cn.datax.service.data.metadata.service.impl;

import cn.datax.common.core.RedisConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.service.data.metadata.api.dto.MetadataAuthorizeDto;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.service.MetadataAuthorizeService;
import cn.datax.service.data.metadata.mapstruct.MetadataAuthorizeMapper;
import cn.datax.service.data.metadata.dao.MetadataAuthorizeDao;
import cn.datax.common.base.BaseServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 数据授权信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-23
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MetadataAuthorizeServiceImpl extends BaseServiceImpl<MetadataAuthorizeDao, MetadataAuthorizeEntity> implements MetadataAuthorizeService {

    @Autowired
    private MetadataAuthorizeDao metadataAuthorizeDao;

    @Autowired
    private MetadataAuthorizeMapper metadataAuthorizeMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> getAuthorizedMetadata(String id) {
        List<MetadataAuthorizeEntity> metadataAuthorizeList = metadataAuthorizeDao.selectList(Wrappers.<MetadataAuthorizeEntity>lambdaQuery().eq(MetadataAuthorizeEntity::getRoleId, id));
		return metadataAuthorizeList.stream().map(MetadataAuthorizeEntity::getObjectId).collect(Collectors.toList());
    }

    @Override
    public void metadataAuthorize(MetadataAuthorizeDto metadataAuthorizeDto) {
        // 先删除
        metadataAuthorizeDao.delete(Wrappers.<MetadataAuthorizeEntity>lambdaQuery().eq(MetadataAuthorizeEntity::getRoleId, metadataAuthorizeDto.getRoleId()));
        metadataAuthorizeDto.getAuthorizeDataList().forEach(s -> {
            MetadataAuthorizeEntity metadataAuthorizeEntity = new MetadataAuthorizeEntity();
            metadataAuthorizeEntity.setRoleId(s.getRoleId());
            metadataAuthorizeEntity.setObjectId(s.getObjectId());
            metadataAuthorizeEntity.setObjectType(s.getObjectType());
            metadataAuthorizeDao.insert(metadataAuthorizeEntity);
        });
    }

    @Override
    public void refreshCache() {
        String authorizeKey = RedisConstant.METADATA_AUTHORIZE_KEY;
        Boolean hasAuthorizeKey = redisService.hasKey(authorizeKey);
        if (hasAuthorizeKey) {
            redisService.del(authorizeKey);
        }
        List<MetadataAuthorizeEntity> metadataAuthorizeList = metadataAuthorizeDao.selectList(Wrappers.emptyWrapper());
        Map<String, List<MetadataAuthorizeEntity>> authorizeListMap = metadataAuthorizeList.stream().collect(Collectors.groupingBy(MetadataAuthorizeEntity::getRoleId));
        redisTemplate.opsForHash().putAll(authorizeKey, authorizeListMap);
    }
}
