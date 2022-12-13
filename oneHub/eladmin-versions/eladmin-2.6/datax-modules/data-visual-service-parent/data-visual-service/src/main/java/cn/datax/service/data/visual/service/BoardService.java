package cn.datax.service.data.visual.service;

import cn.datax.service.data.visual.api.entity.BoardEntity;
import cn.datax.service.data.visual.api.dto.BoardDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 可视化看板配置信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
public interface BoardService extends BaseService<BoardEntity> {

    BoardEntity saveBoard(BoardDto board);

    BoardEntity updateBoard(BoardDto board);

    BoardEntity getBoardById(String id);

    void deleteBoardById(String id);

    void deleteBoardBatch(List<String> ids);

    void copyBoard(String id);

    void buildBoard(BoardDto board);
}
