package cn.datax.service.data.metadata.console.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.metadata.api.dto.SqlConsoleDto;
import cn.datax.service.data.metadata.api.vo.SqlConsoleVo;
import cn.datax.service.data.metadata.console.service.SqlConsoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sql")
public class SqlConsoleController extends BaseController {

    @Autowired
    private SqlConsoleService sqlConsoleService;

    @PostMapping("/run")
    public R sqlRun(@RequestBody @Validated SqlConsoleDto sqlConsoleDto){
        List<SqlConsoleVo> list = sqlConsoleService.sqlRun(sqlConsoleDto);
        return R.ok().setData(list);
    }

    @PostMapping("/stop")
    public R sqlStop(@RequestBody @Validated({ValidationGroups.Other.class}) SqlConsoleDto sqlConsoleDto){
        sqlConsoleService.sqlStop(sqlConsoleDto);
        return R.ok();
    }
}
