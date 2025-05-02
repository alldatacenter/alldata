package cn.datax.service.system.service;

import cn.datax.service.system.api.entity.LoginLogEntity;
import cn.datax.common.base.BaseService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 登录日志信息表 服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
public interface LoginLogService extends BaseService<LoginLogEntity> {

    void saveLoginLog(HttpServletRequest request);

    LoginLogEntity getLoginLogById(String id);

    void deleteLoginLogById(String id);

    void deleteLoginLogBatch(List<String> ids);
}
