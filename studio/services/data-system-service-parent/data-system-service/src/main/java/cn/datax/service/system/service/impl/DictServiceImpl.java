package cn.datax.service.system.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.service.system.api.entity.DictEntity;
import cn.datax.service.system.api.dto.DictDto;
import cn.datax.service.system.api.entity.DictItemEntity;
import cn.datax.service.system.service.DictService;
import cn.datax.service.system.mapstruct.DictMapper;
import cn.datax.service.system.dao.DictDao;
import cn.datax.common.base.BaseServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 字典编码信息表 服务实现类
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DictServiceImpl extends BaseServiceImpl<DictDao, DictEntity> implements DictService {

    @Autowired
    private DictDao dictDao;

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictEntity saveDict(DictDto dictDto) {
        DictEntity dict = dictMapper.toEntity(dictDto);
        int n = dictDao.selectCount(Wrappers.<DictEntity>lambdaQuery().eq(DictEntity::getDictCode, dict.getDictCode()));
        if(n > 0){
            throw new DataException("该字典编码已存在");
        }
        dictDao.insert(dict);
        return dict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictEntity updateDict(DictDto dictDto) {
        DictEntity dict = dictMapper.toEntity(dictDto);
        dictDao.updateById(dict);
        return dict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictById(String id) {
        dictDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictBatch(List<String> ids) {
        dictDao.deleteBatchIds(ids);
    }

    @Override
    public List<DictItemEntity> getDictItems(String code) {
        List<DictEntity> dictEntityList = (List<DictEntity>) redisService.get(RedisConstant.SYSTEM_DICT_KEY);
        Optional<DictEntity> dictEntityOptional = dictEntityList.stream().filter(s -> code.equals(s.getDictCode())).findFirst();
        if (dictEntityOptional.isPresent()) {
            return dictEntityOptional.get().getItems();
        }
        return null;
    }

    @Override
    public void refreshDict() {
        String key = RedisConstant.SYSTEM_DICT_KEY;
        Boolean hasKey = redisService.hasKey(key);
        if (hasKey) {
            redisService.del(key);
        }
        List<DictEntity> dictEntityList = dictDao.queryDictList(DataConstant.EnableState.ENABLE.getKey());
        redisService.set(key, dictEntityList);
    }
}
