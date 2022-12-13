package cn.datax.service.data.quality.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.security.annotation.DataInner;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.quality.api.dto.CheckRuleDto;
import cn.datax.service.data.quality.api.entity.CheckRuleEntity;
import cn.datax.service.data.quality.api.vo.CheckRuleVo;
import cn.datax.service.data.quality.api.query.CheckRuleQuery;
import cn.datax.service.data.quality.mapstruct.CheckRuleMapper;
import cn.datax.service.data.quality.service.CheckRuleService;
import cn.hutool.core.util.StrUtil;
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
 * 核查规则信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Api(tags = {"核查规则信息表"})
@RestController
@RequestMapping("/checkRules")
public class CheckRuleController extends BaseController {

    @Autowired
    private CheckRuleService checkRuleService;

    @Autowired
    private CheckRuleMapper checkRuleMapper;


    @DataInner
	@GetMapping("/source/{sourceId}")
	public CheckRuleEntity getBySourceId(@PathVariable String sourceId) {
		return checkRuleService.getBySourceId(sourceId);
	}

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getCheckRuleById(@PathVariable String id) {
        CheckRuleEntity checkRuleEntity = checkRuleService.getCheckRuleById(id);
        return R.ok().setData(checkRuleMapper.toVO(checkRuleEntity));
    }

    /**
     * 分页查询信息
     *
     * @param checkRuleQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "checkRuleQuery", value = "查询实体checkRuleQuery", required = true, dataTypeClass = CheckRuleQuery.class)
    })
    @GetMapping("/page")
    public R getCheckRulePage(CheckRuleQuery checkRuleQuery) {
        QueryWrapper<CheckRuleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(checkRuleQuery.getRuleTypeId()), "r.rule_type_id", checkRuleQuery.getRuleTypeId());
        queryWrapper.like(StrUtil.isNotBlank(checkRuleQuery.getRuleName()), "r.rule_name", checkRuleQuery.getRuleName());
        queryWrapper.like(StrUtil.isNotBlank(checkRuleQuery.getRuleSource()), "r.rule_source", checkRuleQuery.getRuleSource());
        queryWrapper.like(StrUtil.isNotBlank(checkRuleQuery.getRuleTable()), "r.rule_table", checkRuleQuery.getRuleTable());
        queryWrapper.like(StrUtil.isNotBlank(checkRuleQuery.getRuleColumn()), "r.rule_column", checkRuleQuery.getRuleColumn());
        IPage<CheckRuleEntity> page = checkRuleService.page(new Page<>(checkRuleQuery.getPageNum(), checkRuleQuery.getPageSize()), queryWrapper);
        List<CheckRuleVo> collect = page.getRecords().stream().map(checkRuleMapper::toVO).collect(Collectors.toList());
        JsonPage<CheckRuleVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param checkRule
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据checkRule对象添加信息")
    @ApiImplicitParam(name = "checkRule", value = "详细实体checkRule", required = true, dataType = "CheckRuleDto")
    @PostMapping()
    public R saveCheckRule(@RequestBody @Validated({ValidationGroups.Insert.class}) CheckRuleDto checkRule) {
        CheckRuleEntity checkRuleEntity = checkRuleService.saveCheckRule(checkRule);
        return R.ok().setData(checkRuleMapper.toVO(checkRuleEntity));
    }

    /**
     * 修改
     * @param checkRule
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "checkRule", value = "详细实体checkRule", required = true, dataType = "CheckRuleDto")
    })
    @PutMapping("/{id}")
    public R updateCheckRule(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) CheckRuleDto checkRule) {
        CheckRuleEntity checkRuleEntity = checkRuleService.updateCheckRule(checkRule);
        return R.ok().setData(checkRuleMapper.toVO(checkRuleEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteCheckRuleById(@PathVariable String id) {
        checkRuleService.deleteCheckRuleById(id);
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
    public R deleteCheckRuleBatch(@PathVariable List<String> ids) {
        checkRuleService.deleteCheckRuleBatch(ids);
        return R.ok();
    }
}
