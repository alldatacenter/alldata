package cn.datax.service.system.controller;

import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.DeptDto;
import cn.datax.service.system.api.entity.DeptEntity;
import cn.datax.service.system.api.vo.DeptVo;
import cn.datax.service.system.mapstruct.DeptMapper;
import cn.datax.service.system.service.DeptService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
 *  前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-09-04
 */
@Api(value="系统管理接口", tags = {"系统管理"})
@RestController
@RequestMapping("/depts")
public class DeptController extends BaseController {

    @Autowired
    private DeptService deptService;

    @Autowired
    private DeptMapper deptMapper;

    @ApiOperation(value = "获取部门详细信息", notes = "根据url的id来获取部门详细信息")
    @ApiImplicitParam(name = "id", value = "部门ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getDeptById(@PathVariable String id) {
        DeptEntity deptEntity = deptService.getById(id);
        return R.ok().setData(deptMapper.toVO(deptEntity));
    }

    @ApiOperation(value = "获取部门列表", notes = "")
    @GetMapping("/list")
    public R getDeptList() {
        List<DeptEntity> list = deptService.list(Wrappers.emptyWrapper());
        List<DeptVo> collect = list.stream().map(deptMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    @ApiOperation(value = "创建部门", notes = "根据dept对象创建部门")
    @ApiImplicitParam(name = "dept", value = "部门详细实体dept", required = true, dataType = "DeptDto")
    @PostMapping()
    public R saveDept(@RequestBody @Validated({ValidationGroups.Insert.class}) DeptDto dept) {
        DeptEntity deptEntity = deptService.saveDept(dept);
        return R.ok().setData(deptMapper.toVO(deptEntity));
    }

    @ApiOperation(value = "更新部门详细信息", notes = "根据url的id来指定更新对象，并根据传过来的dept信息来更新部门详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "部门ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "dept", value = "部门详细实体dept", required = true, dataType = "DeptDto")
    })
    @PutMapping("/{id}")
    public R updateDept(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) DeptDto dept) {
        DeptEntity deptEntity = deptService.updateDept(dept);
        return R.ok().setData(deptMapper.toVO(deptEntity));
    }

    @ApiOperation(value = "删除部门", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "部门ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteDept(@PathVariable String id) {
        deptService.deleteDeptById(id);
        return R.ok();
    }
}

