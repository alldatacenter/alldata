package cn.datax.service.data.metadata.console.service;

import cn.datax.service.data.metadata.api.dto.SqlConsoleDto;
import cn.datax.service.data.metadata.api.vo.SqlConsoleVo;

import java.util.List;

public interface SqlConsoleService {
    
    List<SqlConsoleVo> sqlRun(SqlConsoleDto sqlConsoleDto);

    void sqlStop(SqlConsoleDto sqlConsoleDto);
}
