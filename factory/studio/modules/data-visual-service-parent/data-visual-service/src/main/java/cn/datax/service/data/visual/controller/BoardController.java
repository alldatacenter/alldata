package cn.datax.service.data.visual.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.visual.api.dto.BoardDto;
import cn.datax.service.data.visual.api.entity.BoardEntity;
import cn.datax.service.data.visual.api.vo.BoardVo;
import cn.datax.service.data.visual.api.query.BoardQuery;
import cn.datax.service.data.visual.mapstruct.BoardMapper;
import cn.datax.service.data.visual.service.BoardService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 可视化看板配置信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Api(tags = {"可视化看板配置信息表"})
@RestController
@RequestMapping("/boards")
public class BoardController extends BaseController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardMapper boardMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getBoardById(@PathVariable String id) {
        BoardEntity boardEntity = boardService.getBoardById(id);
        return R.ok().setData(boardMapper.toVO(boardEntity));
    }

    /**
     * 分页查询信息
     *
     * @param boardQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "boardQuery", value = "查询实体boardQuery", required = true, dataTypeClass = BoardQuery.class)
    })
    @GetMapping("/page")
    public R getBoardPage(BoardQuery boardQuery) {
        QueryWrapper<BoardEntity> queryWrapper = new QueryWrapper<>();
        IPage<BoardEntity> page = boardService.page(new Page<>(boardQuery.getPageNum(), boardQuery.getPageSize()), queryWrapper);
        List<BoardVo> collect = page.getRecords().stream().map(boardMapper::toVO).collect(Collectors.toList());
        JsonPage<BoardVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param board
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据board对象添加信息")
    @ApiImplicitParam(name = "board", value = "详细实体board", required = true, dataType = "BoardDto")
    @PostMapping()
    public R saveBoard(@RequestBody @Validated({ValidationGroups.Insert.class}) BoardDto board) {
        BoardEntity boardEntity = boardService.saveBoard(board);
        return R.ok().setData(boardMapper.toVO(boardEntity));
    }

    /**
     * 修改
     * @param board
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "board", value = "详细实体board", required = true, dataType = "BoardDto")
    })
    @PutMapping("/{id}")
    public R updateBoard(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) BoardDto board) {
        BoardEntity boardEntity = boardService.updateBoard(board);
        return R.ok().setData(boardMapper.toVO(boardEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteBoardById(@PathVariable String id) {
        boardService.deleteBoardById(id);
        return R.ok();
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteBoardBatch(@PathVariable List<String> ids) {
        boardService.deleteBoardBatch(ids);
        return R.ok();
    }

    @PostMapping("/copy/{id}")
    public R copyBoard(@PathVariable String id) {
        boardService.copyBoard(id);
        return R.ok();
    }

    @PutMapping("/build/{id}")
    public R buildBoard(@PathVariable String id, @RequestBody BoardDto board) {
        boardService.buildBoard(board);
        return R.ok();
    }
}
