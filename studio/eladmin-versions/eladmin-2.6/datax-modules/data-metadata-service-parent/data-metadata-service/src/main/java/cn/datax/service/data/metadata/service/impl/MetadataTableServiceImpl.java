package cn.datax.service.data.metadata.service.impl;

import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.dto.MetadataTableDto;
import cn.datax.service.data.metadata.api.enums.DataLevel;
import cn.datax.service.data.metadata.api.query.MetadataTableQuery;
import cn.datax.service.data.metadata.service.MetadataTableService;
import cn.datax.service.data.metadata.mapstruct.MetadataTableMapper;
import cn.datax.service.data.metadata.dao.MetadataTableDao;
import cn.datax.common.base.BaseServiceImpl;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 数据库表信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MetadataTableServiceImpl extends BaseServiceImpl<MetadataTableDao, MetadataTableEntity> implements MetadataTableService {

    @Autowired
    private MetadataTableDao metadataTableDao;

    @Autowired
    private MetadataTableMapper metadataTableMapper;

    @Autowired
    private RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataTableEntity saveMetadataTable(MetadataTableDto metadataTableDto) {
        MetadataTableEntity metadataTableEntity = metadataTableMapper.toEntity(metadataTableDto);
        metadataTableDao.insert(metadataTableEntity);
        return metadataTableEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataTableEntity updateMetadataTable(MetadataTableDto metadataTableDto) {
        MetadataTableEntity metadataTableEntity = metadataTableMapper.toEntity(metadataTableDto);
        metadataTableDao.updateById(metadataTableEntity);
        return metadataTableEntity;
    }

    @Override
    public MetadataTableEntity getMetadataTableById(String id) {
        MetadataTableEntity metadataTableEntity = super.getById(id);
        return metadataTableEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetadataTableById(String id) {
        metadataTableDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetadataTableBatch(List<String> ids) {
        metadataTableDao.deleteBatchIds(ids);
    }

    @Override
    public List<MetadataTableEntity> getDataMetadataTableList(MetadataTableQuery metadataTableQuery) {
        boolean admin = SecurityUtil.isAdmin();
        if (StrUtil.isBlank(metadataTableQuery.getSourceId())) {
            throw new DataException("数据源不能为空");
        }
        List<MetadataTableEntity> tableList = (List<MetadataTableEntity>) redisService.hget(RedisConstant.METADATA_TABLE_KEY, metadataTableQuery.getSourceId());
        Stream<MetadataTableEntity> stream = Optional.ofNullable(tableList).orElseGet(ArrayList::new).stream();
        if (!admin) {
            Set<String> set = new HashSet<>();
            List<String> roleIds = SecurityUtil.getUserRoleIds();
            roleIds.stream().forEach(role -> {
                List<MetadataAuthorizeEntity> list = (List<MetadataAuthorizeEntity>) redisService.hget(RedisConstant.METADATA_AUTHORIZE_KEY, role);
                set.addAll(Optional.ofNullable(list).orElseGet(ArrayList::new).stream()
                        .filter(s -> Objects.equals(DataLevel.TABLE.getKey(), s.getObjectType()))
                        .map(s -> s.getObjectId()).collect(Collectors.toSet()));
            });
            stream = stream.filter(s -> set.contains(s.getId()));
        }
        return stream.collect(Collectors.toList());
    }

    @Override
    public <E extends IPage<MetadataTableEntity>> E pageWithAuth(E page, Wrapper<MetadataTableEntity> queryWrapper) {
        boolean admin = SecurityUtil.isAdmin();
        List<String> roles = new ArrayList<>();
        if (!admin) {
            roles = SecurityUtil.getUserRoleIds();
        }
        return metadataTableDao.selectPageWithAuth(page, queryWrapper, roles);
    }
}
