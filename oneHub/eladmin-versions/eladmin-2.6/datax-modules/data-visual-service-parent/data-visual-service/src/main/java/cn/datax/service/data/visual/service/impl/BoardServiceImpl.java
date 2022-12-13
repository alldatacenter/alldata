package cn.datax.service.data.visual.service.impl;

import cn.datax.common.core.DataConstant;
import cn.datax.common.exception.DataException;
import cn.datax.service.data.visual.api.entity.BoardChartEntity;
import cn.datax.service.data.visual.api.entity.BoardEntity;
import cn.datax.service.data.visual.api.dto.BoardDto;
import cn.datax.service.data.visual.dao.BoardChartDao;
import cn.datax.service.data.visual.service.BoardService;
import cn.datax.service.data.visual.mapstruct.BoardMapper;
import cn.datax.service.data.visual.dao.BoardDao;
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
 * 可视化看板配置信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class BoardServiceImpl extends BaseServiceImpl<BoardDao, BoardEntity> implements BoardService {

	@Autowired
	private BoardDao boardDao;

	@Autowired
	private BoardMapper boardMapper;

	@Autowired
	private BoardChartDao boardChartDao;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BoardEntity saveBoard(BoardDto boardDto) {
		BoardEntity board = boardMapper.toEntity(boardDto);
		boardDao.insert(board);
		return board;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BoardEntity updateBoard(BoardDto boardDto) {
		BoardEntity board = boardMapper.toEntity(boardDto);
		boardDao.updateById(board);
		return board;
	}

	@Override
	public BoardEntity getBoardById(String id) {
		BoardEntity boardEntity = super.getById(id);
		return boardEntity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBoardById(String id) {
		//zrx 删除与chart的关联信息
		boardChartDao.delete(new QueryWrapper<BoardChartEntity>().eq("board_id", id));
		boardDao.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteBoardBatch(List<String> ids) {
		//zrx
		for (String id : ids) {
			deleteBoardById(id);
		}
		//boardDao.deleteBatchIds(ids);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void copyBoard(String id) {
		BoardEntity boardEntity = Optional.ofNullable(super.getById(id)).orElseThrow(() -> new DataException("获取失败"));
		BoardEntity copy = new BoardEntity();
		copy.setBoardName(boardEntity.getBoardName() + "_副本" + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
		copy.setBoardThumbnail(boardEntity.getBoardThumbnail());
		copy.setBoardConfig(boardEntity.getBoardConfig());
		copy.setStatus(DataConstant.EnableState.ENABLE.getKey());
		boardDao.insert(copy);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void buildBoard(BoardDto boardDto) {
		BoardEntity board = boardMapper.toEntity(boardDto);
		boardDao.updateById(board);
		boardChartDao.delete(Wrappers.<BoardChartEntity>lambdaQuery()
				.eq(BoardChartEntity::getBoardId, boardDto.getId()));
		List<BoardChartEntity> boardChartEntityList = Optional.ofNullable(board.getBoardConfig().getLayout()).orElse(new ArrayList<>())
				.stream().map(s -> {
					BoardChartEntity boardChartEntity = new BoardChartEntity();
					boardChartEntity.setBoardId(boardDto.getId());
					boardChartEntity.setChartId(s.getI());
					return boardChartEntity;
				}).collect(Collectors.toList());
		boardChartDao.insertBatch(boardChartEntityList);
	}
}
