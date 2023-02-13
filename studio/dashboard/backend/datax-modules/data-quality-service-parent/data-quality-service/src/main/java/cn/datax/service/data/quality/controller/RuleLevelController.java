package cn.datax.service.data.quality.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.data.quality.api.entity.RuleLevelEntity;
import cn.datax.service.data.quality.api.vo.RuleLevelVo;
import cn.datax.service.data.quality.api.query.RuleLevelQuery;
import cn.datax.service.data.quality.mapstruct.RuleLevelMapper;
import cn.datax.service.data.quality.service.RuleLevelService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 规则级别信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Api(tags = {"规则级别信息表"})
@RestController
@RequestMapping("/ruleLevels")
public class RuleLevelController extends BaseController {

    @Autowired
    private RuleLevelService ruleLevelService;

    @Autowired
    private RuleLevelMapper ruleLevelMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getRuleLevelById(@PathVariable String id) {
        RuleLevelEntity ruleLevelEntity = ruleLevelService.getRuleLevelById(id);
        return R.ok().setData(ruleLevelMapper.toVO(ruleLevelEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getRuleTypeList() {
        List<RuleLevelEntity> list = ruleLevelService.list(Wrappers.emptyWrapper());
        return R.ok().setData(list);
    }

    /**
     * 分页查询信息
     *
     * @param ruleLevelQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ruleLevelQuery", value = "查询实体ruleLevelQuery", required = true, dataTypeClass = RuleLevelQuery.class)
    })
    @GetMapping("/page")
    public R getRuleLevelPage(RuleLevelQuery ruleLevelQuery) {
        QueryWrapper<RuleLevelEntity> queryWrapper = new QueryWrapper<>();
        IPage<RuleLevelEntity> page = ruleLevelService.page(new Page<>(ruleLevelQuery.getPageNum(), ruleLevelQuery.getPageSize()), queryWrapper);
        List<RuleLevelVo> collect = page.getRecords().stream().map(ruleLevelMapper::toVO).collect(Collectors.toList());
        JsonPage<RuleLevelVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }
}
