package cn.datax.service.system.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.service.system.api.entity.LoginLogEntity;
import cn.datax.service.system.api.vo.LoginLogVo;
import cn.datax.service.system.api.query.LoginLogQuery;
import cn.datax.service.system.mapstruct.LoginLogMapper;
import cn.datax.service.system.service.LoginLogService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 登录日志信息表 前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@Api(tags = {"登录日志信息表"})
@RestController
@RequestMapping("/login/logs")
public class LoginLogController extends BaseController {

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private LoginLogMapper loginLogMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getLoginLogById(@PathVariable String id) {
        LoginLogEntity loginLogEntity = loginLogService.getLoginLogById(id);
        return R.ok().setData(loginLogMapper.toVO(loginLogEntity));
    }

    /**
     * 分页查询信息
     *
     * @param loginLogQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "loginLogQuery", value = "查询实体loginLogQuery", required = true, dataTypeClass = LoginLogQuery.class)
    })
    @GetMapping("/page")
    public R getLoginLogPage(LoginLogQuery loginLogQuery) {
        QueryWrapper<LoginLogEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(loginLogQuery.getUserName()), "user_name", loginLogQuery.getUserName());
        IPage<LoginLogEntity> page = loginLogService.page(new Page<>(loginLogQuery.getPageNum(), loginLogQuery.getPageSize()), queryWrapper);
        List<LoginLogVo> collect = page.getRecords().stream().map(loginLogMapper::toVO).collect(Collectors.toList());
        JsonPage<LoginLogVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加登录日志
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据request对象添加信息")
    @PostMapping()
    public R loginLog(HttpServletRequest request) {
        loginLogService.saveLoginLog(request);
        return R.ok();
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteLoginLogById(@PathVariable String id) {
        loginLogService.deleteLoginLogById(id);
        return R.ok();
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除角色", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteLoginLogBatch(@PathVariable List<String> ids) {
        loginLogService.deleteLoginLogBatch(ids);
        return R.ok();
    }
}
