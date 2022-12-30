package cn.datax.service.data.quality.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.data.quality.api.entity.RuleItemEntity;
import cn.datax.service.data.quality.api.vo.RuleItemVo;
import cn.datax.service.data.quality.api.query.RuleItemQuery;
import cn.datax.service.data.quality.mapstruct.RuleItemMapper;
import cn.datax.service.data.quality.service.RuleItemService;
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
 * 规则核查项信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Api(tags = {"规则核查项信息表"})
@RestController
@RequestMapping("/ruleItems")
public class RuleItemController extends BaseController {

    @Autowired
    private RuleItemService ruleItemService;

    @Autowired
    private RuleItemMapper ruleItemMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getRuleItemById(@PathVariable String id) {
        RuleItemEntity ruleItemEntity = ruleItemService.getRuleItemById(id);
        return R.ok().setData(ruleItemMapper.toVO(ruleItemEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getRuleTypeList(RuleItemQuery ruleItemQuery) {
        QueryWrapper<RuleItemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(ruleItemQuery.getRuleTypeId()), "rule_type_id", ruleItemQuery.getRuleTypeId());
        List<RuleItemEntity> list = ruleItemService.list(queryWrapper);
        return R.ok().setData(list);
    }

    /**
     * 分页查询信息
     *
     * @param ruleItemQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ruleItemQuery", value = "查询实体ruleItemQuery", required = true, dataTypeClass = RuleItemQuery.class)
    })
    @GetMapping("/page")
    public R getRuleItemPage(RuleItemQuery ruleItemQuery) {
        QueryWrapper<RuleItemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(ruleItemQuery.getRuleTypeId()), "rule_type_id", ruleItemQuery.getRuleTypeId());
        IPage<RuleItemEntity> page = ruleItemService.page(new Page<>(ruleItemQuery.getPageNum(), ruleItemQuery.getPageSize()), queryWrapper);
        List<RuleItemVo> collect = page.getRecords().stream().map(ruleItemMapper::toVO).collect(Collectors.toList());
        JsonPage<RuleItemVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }
}
