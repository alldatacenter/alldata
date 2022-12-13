package cn.datax.service.data.visual.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.exception.DataException;
import cn.datax.service.data.visual.api.entity.ScreenChartEntity;
import cn.datax.service.data.visual.api.entity.ScreenEntity;
import cn.datax.service.data.visual.api.dto.ScreenDto;
import cn.datax.service.data.visual.dao.ScreenChartDao;
import cn.datax.service.data.visual.service.ScreenService;
import cn.datax.service.data.visual.mapstruct.ScreenMapper;
import cn.datax.service.data.visual.dao.ScreenDao;
import cn.datax.common.base.BaseServiceImpl;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * 可视化酷屏配置信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ScreenServiceImpl extends BaseServiceImpl<ScreenDao, ScreenEntity> implements ScreenService {

	@Autowired
	private ScreenDao screenDao;

	@Autowired
	private ScreenMapper screenMapper;

	@Autowired
	private ScreenChartDao screenChartDao;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ScreenEntity saveScreen(ScreenDto screenDto) {
		ScreenEntity screen = screenMapper.toEntity(screenDto);
		screenDao.insert(screen);
		return screen;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ScreenEntity updateScreen(ScreenDto screenDto) {
		ScreenEntity screen = screenMapper.toEntity(screenDto);
		screenDao.updateById(screen);
		return screen;
	}

	@Override
	public ScreenEntity getScreenById(String id) {
		ScreenEntity screenEntity = super.getById(id);
		return screenEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteScreenById(String id) {
		//zrx 删除与chart的关联信息
		screenChartDao.delete(new QueryWrapper<ScreenChartEntity>().eq("screen_id", id));
		screenDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteScreenBatch(List<String> ids) {
		//zrx
		for (String id : ids) {
			deleteScreenById(id);
		}
		//screenDao.deleteBatchIds(ids);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void copyScreen(String id) {
		ScreenEntity screenEntity = Optional.ofNullable(super.getById(id)).orElseThrow(() -> new DataException("获取失败"));
		ScreenEntity copy = new ScreenEntity();
		copy.setScreenName(screenEntity.getScreenName() + "_副本" + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
		copy.setScreenThumbnail(screenEntity.getScreenThumbnail());
		copy.setScreenConfig(screenEntity.getScreenConfig());
		copy.setStatus(DataConstant.EnableState.ENABLE.getKey());
		screenDao.insert(copy);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void buildScreen(ScreenDto screenDto) {
		ScreenEntity screen = screenMapper.toEntity(screenDto);
		screenDao.updateById(screen);
		screenChartDao.delete(Wrappers.<ScreenChartEntity>lambdaQuery()
				.eq(ScreenChartEntity::getScreenId, screenDto.getId()));
		List<ScreenChartEntity> screenChartEntityList = Optional.ofNullable(screen.getScreenConfig().getLayout()).orElse(new ArrayList<>())
				.stream().map(s -> {
					ScreenChartEntity screenChartEntity = new ScreenChartEntity();
					screenChartEntity.setScreenId(screenDto.getId());
					screenChartEntity.setChartId(s.getI());
					return screenChartEntity;
				}).collect(Collectors.toList());
		screenChartDao.insertBatch(screenChartEntityList);
	}
}
