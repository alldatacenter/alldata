package cn.datax.service.system.service.impl;

import cn.datax.common.core.DataUser;
import cn.datax.common.utils.IPUtil;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.system.api.dto.JwtUserDto;
import cn.datax.service.system.api.entity.LoginLogEntity;
import cn.datax.service.system.service.LoginLogService;
import cn.datax.service.system.mapstruct.LoginLogMapper;
import cn.datax.service.system.dao.LoginLogDao;
import cn.datax.common.base.BaseServiceImpl;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 登录日志信息表 服务实现类
 * </p>
 *
 * @author yuwei
 * @date 2022-05-29
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class LoginLogServiceImpl extends BaseServiceImpl<LoginLogDao, LoginLogEntity> implements LoginLogService {

    @Autowired
    private LoginLogDao loginLogDao;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLoginLog(HttpServletRequest request) {
        String ip = IPUtil.getIpAddr(request);
        UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
        String os = userAgent.getOperatingSystem().getName();
        String browser = userAgent.getBrowser().getName();
        JwtUserDto user = SecurityUtil.getDataUser();
        String username = user.getUsername();
        LoginLogEntity loginLog = new LoginLogEntity();
        loginLog.setOpIp(ip).setOpOs(os).setOpBrowser(browser).setUserId("test").setUserName(username).setOpDate(LocalDateTime.now());
        loginLogDao.insert(loginLog);
    }

    @Override
    public LoginLogEntity getLoginLogById(String id) {
        LoginLogEntity loginLogEntity = super.getById(id);
        return loginLogEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLoginLogById(String id) {
        loginLogDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLoginLogBatch(List<String> ids) {
        loginLogDao.deleteBatchIds(ids);
    }
}
