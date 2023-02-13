package cn.datax.auth.listener;

import cn.datax.common.utils.IPUtil;
import cn.datax.common.utils.RequestHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class AuthenticationSuccessEventListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        if(!event.getSource().getClass().getName().equals("org.springframework.security.authentication.UsernamePasswordAuthenticationToken")){
            HttpServletRequest httpServletRequest = RequestHolder.getHttpServletRequest();
            String ipAddr = IPUtil.getIpAddr(httpServletRequest);
            log.info("{}登录成功:地址{}", event.getAuthentication().getPrincipal(), ipAddr);
        }
    }
}
