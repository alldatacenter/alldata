package cn.datax.service.data.quality.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.data.quality.api.entity.RuleTypeEntity;
import cn.datax.service.data.quality.api.entity.RuleTypeReportEntity;
import cn.datax.service.data.quality.api.vo.RuleTypeVo;
import cn.datax.service.data.quality.api.query.RuleTypeQuery;
import cn.datax.service.data.quality.mapstruct.RuleTypeMapper;
import cn.datax.service.data.quality.service.RuleTypeService;
import cn.hutool.core.util.StrUtil;
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
 * 规则类型信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Api(tags = {"规则类型信息表"})
@RestController
@RequestMapping("/ruleTypes")
public class RuleTypeController extends BaseController {

    @Autowired
    private RuleTypeService ruleTypeService;

    @Autowired
    private RuleTypeMapper ruleTypeMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getRuleTypeById(@PathVariable String id) {
        RuleTypeEntity ruleTypeEntity = ruleTypeService.getRuleTypeById(id);
        return R.ok().setData(ruleTypeMapper.toVO(ruleTypeEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getRuleTypeList() {
        List<RuleTypeEntity> list = ruleTypeService.list(Wrappers.emptyWrapper());
        return R.ok().setData(list);
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/report/list")
    public R getRuleTypeListForReport() {
        List<RuleTypeReportEntity> list = ruleTypeService.getRuleTypeListForReport();
        return R.ok().setData(list);
    }

    /**
     * 分页查询信息
     *
     * @param ruleTypeQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ruleTypeQuery", value = "查询实体ruleTypeQuery", required = true, dataTypeClass = RuleTypeQuery.class)
    })
    @GetMapping("/page")
    public R getRuleTypePage(RuleTypeQuery ruleTypeQuery) {
        QueryWrapper<RuleTypeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(ruleTypeQuery.getName()), "name", ruleTypeQuery.getName());
        IPage<RuleTypeEntity> page = ruleTypeService.page(new Page<>(ruleTypeQuery.getPageNum(), ruleTypeQuery.getPageSize()), queryWrapper);
        List<RuleTypeVo> collect = page.getRecords().stream().map(ruleTypeMapper::toVO).collect(Collectors.toList());
        JsonPage<RuleTypeVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }
}
