package cn.datax.service.system.service.impl;

import cn.datax.service.system.api.entity.DictItemEntity;
import cn.datax.service.system.api.dto.DictItemDto;
import cn.datax.service.system.service.DictItemService;
import cn.datax.service.system.mapstruct.DictItemMapper;
import cn.datax.service.system.dao.DictItemDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 字典项信息表 服务实现类
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DictItemServiceImpl extends BaseServiceImpl<DictItemDao, DictItemEntity> implements DictItemService {

    @Autowired
    private DictItemDao dictItemDao;

    @Autowired
    private DictItemMapper dictItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictItemEntity saveDictItem(DictItemDto dictItemDto) {
        DictItemEntity dictItem = dictItemMapper.toEntity(dictItemDto);
        dictItemDao.insert(dictItem);
        return dictItem;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictItemEntity updateDictItem(DictItemDto dictItemDto) {
        DictItemEntity dictItem = dictItemMapper.toEntity(dictItemDto);
        dictItemDao.updateById(dictItem);
        return dictItem;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictItemById(String id) {
        dictItemDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictItemBatch(List<String> ids) {
        dictItemDao.deleteBatchIds(ids);
    }
}
