package cn.datax.service.data.standard.service;

import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.dto.ContrastDto;
import cn.datax.common.base.BaseService;
import cn.datax.service.data.standard.api.vo.ContrastTreeVo;
import cn.datax.service.system.api.entity.UserEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * <p>
 * 对照表信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
public interface ContrastService extends BaseService<ContrastEntity> {

    ContrastEntity saveContrast(ContrastDto contrast);

    ContrastEntity updateContrast(ContrastDto contrast);

    ContrastEntity getContrastById(String id);

    void deleteContrastById(String id);

    void deleteContrastBatch(List<String> ids);

    List<ContrastTreeVo> getContrastTree();

    IPage<ContrastEntity> statistic(IPage<ContrastEntity> page, Wrapper<ContrastEntity> queryWrapper);

	ContrastEntity getBySourceId(String sourceId);
}
