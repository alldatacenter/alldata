package cn.datax.service.data.visual.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.visual.api.dto.BoardDto;
import cn.datax.service.data.visual.api.dto.ScreenDto;
import cn.datax.service.data.visual.api.entity.ScreenEntity;
import cn.datax.service.data.visual.api.vo.ScreenVo;
import cn.datax.service.data.visual.api.query.ScreenQuery;
import cn.datax.service.data.visual.mapstruct.ScreenMapper;
import cn.datax.service.data.visual.service.ScreenService;
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
 * 可视化酷屏配置信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Api(tags = {"可视化酷屏配置信息表"})
@RestController
@RequestMapping("/screens")
public class ScreenController extends BaseController {

    @Autowired
    private ScreenService screenService;

    @Autowired
    private ScreenMapper screenMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getScreenById(@PathVariable String id) {
        ScreenEntity screenEntity = screenService.getScreenById(id);
        return R.ok().setData(screenMapper.toVO(screenEntity));
    }

    /**
     * 分页查询信息
     *
     * @param screenQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "screenQuery", value = "查询实体screenQuery", required = true, dataTypeClass = ScreenQuery.class)
    })
    @GetMapping("/page")
    public R getScreenPage(ScreenQuery screenQuery) {
        QueryWrapper<ScreenEntity> queryWrapper = new QueryWrapper<>();
        IPage<ScreenEntity> page = screenService.page(new Page<>(screenQuery.getPageNum(), screenQuery.getPageSize()), queryWrapper);
        List<ScreenVo> collect = page.getRecords().stream().map(screenMapper::toVO).collect(Collectors.toList());
        JsonPage<ScreenVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param screen
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据screen对象添加信息")
    @ApiImplicitParam(name = "screen", value = "详细实体screen", required = true, dataType = "ScreenDto")
    @PostMapping()
    public R saveScreen(@RequestBody @Validated({ValidationGroups.Insert.class}) ScreenDto screen) {
        ScreenEntity screenEntity = screenService.saveScreen(screen);
        return R.ok().setData(screenMapper.toVO(screenEntity));
    }

    /**
     * 修改
     * @param screen
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "screen", value = "详细实体screen", required = true, dataType = "ScreenDto")
    })
    @PutMapping("/{id}")
    public R updateScreen(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ScreenDto screen) {
        ScreenEntity screenEntity = screenService.updateScreen(screen);
        return R.ok().setData(screenMapper.toVO(screenEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteScreenById(@PathVariable String id) {
        screenService.deleteScreenById(id);
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
    public R deleteScreenBatch(@PathVariable List<String> ids) {
        screenService.deleteScreenBatch(ids);
        return R.ok();
    }

    @PostMapping("/copy/{id}")
    public R copyScreen(@PathVariable String id) {
        screenService.copyScreen(id);
        return R.ok();
    }

    @PutMapping("/build/{id}")
    public R buildScreen(@PathVariable String id, @RequestBody ScreenDto screen) {
        screenService.buildScreen(screen);
        return R.ok();
    }
}
