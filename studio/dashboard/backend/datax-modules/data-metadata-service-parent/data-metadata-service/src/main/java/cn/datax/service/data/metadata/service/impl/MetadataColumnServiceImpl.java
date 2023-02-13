package cn.datax.service.data.metadata.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.data.metadata.api.dto.MetadataColumnDto;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.api.enums.DataLevel;
import cn.datax.service.data.metadata.api.query.MetadataColumnQuery;
import cn.datax.service.data.metadata.api.vo.MetadataTreeVo;
import cn.datax.service.data.metadata.service.MetadataColumnService;
import cn.datax.service.data.metadata.mapstruct.MetadataColumnMapper;
import cn.datax.service.data.metadata.dao.MetadataColumnDao;
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
 * 元数据信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MetadataColumnServiceImpl extends BaseServiceImpl<MetadataColumnDao, MetadataColumnEntity> implements MetadataColumnService {

    @Autowired
    private MetadataColumnDao metadataColumnDao;

    @Autowired
    private MetadataColumnMapper metadataColumnMapper;

    @Autowired
    private RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataColumnEntity saveMetadataColumn(MetadataColumnDto metadataColumnDto) {
        MetadataColumnEntity metadataColumn = metadataColumnMapper.toEntity(metadataColumnDto);
        metadataColumnDao.insert(metadataColumn);
        return metadataColumn;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataColumnEntity updateMetadataColumn(MetadataColumnDto metadataColumnDto) {
        MetadataColumnEntity metadataColumn = metadataColumnMapper.toEntity(metadataColumnDto);
        metadataColumnDao.updateById(metadataColumn);
        return metadataColumn;
    }

    @Override
    public MetadataColumnEntity getMetadataColumnById(String id) {
        MetadataColumnEntity metadataColumnEntity = super.getById(id);
        return metadataColumnEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetadataColumnById(String id) {
        metadataColumnDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetadataColumnBatch(List<String> ids) {
        metadataColumnDao.deleteBatchIds(ids);
    }

    @Override
    public List<MetadataTreeVo> getDataMetadataTree(String level, MetadataColumnQuery metadataColumnQuery) {
        boolean admin = SecurityUtil.isAdmin();
        List<MetadataSourceEntity> sourceList = (List<MetadataSourceEntity>) redisService.get(RedisConstant.METADATA_SOURCE_KEY);
        Stream<MetadataSourceEntity> stream = Optional.ofNullable(sourceList).orElseGet(ArrayList::new).stream().filter(s -> DataConstant.EnableState.ENABLE.getKey().equals(s.getStatus()));
        if (StrUtil.isNotBlank(metadataColumnQuery.getSourceId())) {
            stream = stream.filter(s -> metadataColumnQuery.getSourceId().equals(s.getId()));
        }
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
        List<MetadataTreeVo> list = stream.map(m -> {
            MetadataTreeVo tree = new MetadataTreeVo();
            tree.setId(m.getId());
            tree.setType(DataLevel.DATABASE.getKey());
            tree.setLabel(m.getSourceName());
            tree.setName(m.getSourceName());
            if (DataLevel.getLevel(level).getLevel() >= DataLevel.TABLE.getLevel()) {
                tree.setChildren(getTableChildrens(m.getId(), level, metadataColumnQuery.getTableId()));
            }
            return tree;
        }).collect(Collectors.toList());
        return list;
    }

    private List<MetadataTreeVo> getTableChildrens(String id, String level, String tableId) {
        boolean admin = SecurityUtil.isAdmin();
        List<MetadataTableEntity> tableList = (List<MetadataTableEntity>) redisService.hget(RedisConstant.METADATA_TABLE_KEY, id);
        Stream<MetadataTableEntity> stream = Optional.ofNullable(tableList).orElseGet(ArrayList::new).stream();
        if (StrUtil.isNotBlank(tableId)) {
            stream = stream.filter(s -> tableId.equals(s.getId()));
        }
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
        List<MetadataTreeVo> children = stream.map(m -> {
            MetadataTreeVo tree = new MetadataTreeVo();
            tree.setId(m.getId());
            tree.setType(DataLevel.TABLE.getKey());
            tree.setName(m.getTableComment());
            tree.setCode(m.getTableName());
            tree.setLabel(StrUtil.isBlank(m.getTableComment()) ? m.getTableName() : m.getTableComment());
            if (DataLevel.getLevel(level).getLevel() >= DataLevel.COLUMN.getLevel()) {
                tree.setChildren(getColumnChildrens(m.getId()));
            }
            return tree;
        }).collect(Collectors.toList());
        return children;
    }

    private List<MetadataTreeVo> getColumnChildrens(String id) {
        boolean admin = SecurityUtil.isAdmin();
        List<MetadataColumnEntity> columnList = (List<MetadataColumnEntity>) redisService.hget(RedisConstant.METADATA_COLUMN_KEY, id);
        Stream<MetadataColumnEntity> stream = Optional.ofNullable(columnList).orElseGet(ArrayList::new).stream();
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
        List<MetadataTreeVo> children = stream.map(m -> {
            MetadataTreeVo tree = new MetadataTreeVo();
            tree.setId(m.getId());
            tree.setType(DataLevel.COLUMN.getKey());
            tree.setName(m.getColumnComment());
            tree.setCode(m.getColumnName());
            tree.setLabel(StrUtil.isBlank(m.getColumnComment()) ? m.getColumnName() : m.getColumnComment());
            return tree;
        }).collect(Collectors.toList());
        return children;
    }

    @Override
    public List<MetadataColumnEntity> getDataMetadataColumnList(MetadataColumnQuery metadataColumnQuery) {
        boolean admin = SecurityUtil.isAdmin();
        if (StrUtil.isBlank(metadataColumnQuery.getTableId())) {
            throw new DataException("数据表不能为空");
        }
        List<MetadataColumnEntity> columnList = (List<MetadataColumnEntity>) redisService.hget(RedisConstant.METADATA_COLUMN_KEY, metadataColumnQuery.getTableId());
        Stream<MetadataColumnEntity> stream = Optional.ofNullable(columnList).orElseGet(ArrayList::new).stream();
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
        return stream.collect(Collectors.toList());
    }

    @Override
    public <E extends IPage<MetadataColumnEntity>> E pageWithAuth(E page, Wrapper<MetadataColumnEntity> queryWrapper) {
        boolean admin = SecurityUtil.isAdmin();
        List<String> roles = new ArrayList<>();
        if (!admin) {
            roles = SecurityUtil.getUserRoleIds();
        }
        return metadataColumnDao.selectPageWithAuth(page, queryWrapper, roles);
    }
}
