package cn.datax.service.data.standard.service.impl;

import cn.datax.service.data.standard.api.entity.ContrastDictEntity;
import cn.datax.service.data.standard.api.dto.ContrastDictDto;
import cn.datax.service.data.standard.service.ContrastDictService;
import cn.datax.service.data.standard.mapstruct.ContrastDictMapper;
import cn.datax.service.data.standard.dao.ContrastDictDao;
import cn.datax.common.base.BaseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 字典对照信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ContrastDictServiceImpl extends BaseServiceImpl<ContrastDictDao, ContrastDictEntity> implements ContrastDictService {

    @Autowired
    private ContrastDictDao contrastDictDao;

    @Autowired
    private ContrastDictMapper contrastDictMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContrastDictEntity saveContrastDict(ContrastDictDto contrastDictDto) {
        ContrastDictEntity contrastDict = contrastDictMapper.toEntity(contrastDictDto);
        contrastDictDao.insert(contrastDict);
        return contrastDict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContrastDictEntity updateContrastDict(ContrastDictDto contrastDictDto) {
        ContrastDictEntity contrastDict = contrastDictMapper.toEntity(contrastDictDto);
        contrastDictDao.updateById(contrastDict);
        return contrastDict;
    }

    @Override
    public ContrastDictEntity getContrastDictById(String id) {
        ContrastDictEntity contrastDictEntity = super.getById(id);
        return contrastDictEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContrastDictById(String id) {
        contrastDictDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContrastDictBatch(List<String> ids) {
        contrastDictDao.deleteBatchIds(ids);
    }
}
