package cn.datax.service.data.standard.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.service.data.standard.api.dto.Endpoint;
import cn.datax.service.data.standard.api.dto.ManualMappingDto;
import cn.datax.service.data.standard.api.entity.ContrastDictEntity;
import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.entity.DictEntity;
import cn.datax.service.data.standard.api.vo.ContrastDictVo;
import cn.datax.service.data.standard.api.vo.DictVo;
import cn.datax.service.data.standard.dao.ContrastDao;
import cn.datax.service.data.standard.dao.ContrastDictDao;
import cn.datax.service.data.standard.dao.DictDao;
import cn.datax.service.data.standard.mapstruct.ContrastDictMapper;
import cn.datax.service.data.standard.mapstruct.DictMapper;
import cn.datax.service.data.standard.service.DictMappingService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DictMappingServiceImpl implements DictMappingService {

    @Autowired
    private ContrastDao contrastDao;

    @Autowired
    private ContrastDictDao contrastDictDao;

    @Autowired
    private ContrastDictMapper contrastDictMapper;

    @Autowired
    private DictDao dictDao;

    @Autowired
    private DictMapper dictMapper;

    private static String BIND_GB_CODE = "gb_code";
    private static String BIND_GB_NAME = "gb_name";

    @Override
    public Map<String, Object> getDictMapping(String id) {
        ContrastEntity contrastEntity = contrastDao.selectById(id);
        String contrastId = contrastEntity.getId();
        String gbTypeId = contrastEntity.getGbTypeId();
        QueryWrapper<ContrastDictEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("d.contrast_id", contrastId);
        List<ContrastDictEntity> contrastDictEntityList = contrastDictDao.selectList(queryWrapper);
        List<ContrastDictVo> contrastDictList = contrastDictEntityList.stream().map(contrastDictMapper::toVO).collect(Collectors.toList());
        List<DictEntity> dictEntityList = dictDao.selectList(Wrappers.<DictEntity>lambdaQuery().eq(DictEntity::getTypeId, gbTypeId).eq(DictEntity::getStatus, DataConstant.TrueOrFalse.TRUE.getKey()));
        List<DictVo> dictList = dictEntityList.stream().map(dictMapper::toVO).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>(4);
        String tableName = StrUtil.isBlank(contrastEntity.getTableComment()) ? contrastEntity.getTableName() : contrastEntity.getTableName() + "(" + contrastEntity.getTableComment() + ")";
        String columnName = StrUtil.isBlank(contrastEntity.getColumnComment()) ? contrastEntity.getTableName() : contrastEntity.getColumnName() + "(" + contrastEntity.getColumnComment() + ")";
        long contrastTotal = contrastDictList.stream().count();
        long unContrastTotal = contrastDictList.stream().filter(s -> DataConstant.TrueOrFalse.FALSE.getKey().equals(s.getStatus())).count();
        map.put("title", "数据源: " + contrastEntity.getSourceName() + " 数据表: " + tableName + " 对照字段: " + columnName + " 标准类别编码: " + contrastEntity.getGbTypeCode() + " 标准类别名称: " + contrastEntity.getGbTypeName());
        map.put("description", "总数: " + contrastTotal + " 未对照: " + unContrastTotal + " 已对照: " + (contrastTotal - unContrastTotal));
        map.put("left", contrastDictList);
        map.put("right", dictList);
        return map;
    }

    @Override
    public void dictAutoMapping(String id) {
        ContrastEntity contrastEntity = contrastDao.selectById(id);
        String contrastId = contrastEntity.getId();
        String gbTypeId = contrastEntity.getGbTypeId();
        String bindGbColumn = contrastEntity.getBindGbColumn();
        // 查询未对照数据
        QueryWrapper<ContrastDictEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("d.contrast_id", contrastId);
        queryWrapper.eq("d.status", DataConstant.TrueOrFalse.FALSE.getKey());
        List<ContrastDictEntity> contrastDictEntityList = contrastDictDao.selectList(queryWrapper);
        // 查询标准字典数据
        List<DictEntity> dictEntityList = dictDao.selectList(Wrappers.<DictEntity>lambdaQuery().eq(DictEntity::getTypeId, gbTypeId).eq(DictEntity::getStatus, DataConstant.TrueOrFalse.TRUE.getKey()));
        contrastDictEntityList.stream().forEach(c -> {
            dictEntityList.stream().filter(d -> {
                if (BIND_GB_CODE.equals(bindGbColumn)) {
                    return Objects.equals(c.getColCode(), d.getGbCode());
                } else {
                    return Objects.equals(c.getColName(), d.getGbName());
                }
            }).forEach(s -> {
                // 更新对照结果
                String contrastGbId = s.getId();
                c.setStatus(DataConstant.TrueOrFalse.TRUE.getKey());
                c.setContrastGbId(contrastGbId);
                contrastDictDao.updateById(c);
            });
        });
    }

    @Override
    public void dictManualMapping(ManualMappingDto manualMappingDto) {
        List<Endpoint> endpoints = manualMappingDto.getEndpoints();
        endpoints.stream().forEach(s -> {
            LambdaUpdateWrapper<ContrastDictEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(ContrastDictEntity::getStatus, DataConstant.TrueOrFalse.TRUE.getKey());
            updateWrapper.set(ContrastDictEntity::getContrastGbId, s.getTargetId());
            updateWrapper.eq(ContrastDictEntity::getId, s.getSourceId());
            contrastDictDao.update(null, updateWrapper);
        });
    }

    @Override
    public void dictCancelMapping(String id) {
        LambdaUpdateWrapper<ContrastDictEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(ContrastDictEntity::getStatus, DataConstant.TrueOrFalse.FALSE.getKey());
        updateWrapper.set(ContrastDictEntity::getContrastGbId, null);
        updateWrapper.eq(ContrastDictEntity::getId, id);
        contrastDictDao.update(null, updateWrapper);
    }
}
