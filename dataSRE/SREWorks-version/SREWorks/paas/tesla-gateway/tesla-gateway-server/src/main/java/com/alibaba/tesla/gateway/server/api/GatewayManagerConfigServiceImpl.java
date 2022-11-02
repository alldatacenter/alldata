package com.alibaba.tesla.gateway.server.api;

import com.alibaba.tesla.gateway.api.GatewayManagerConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class GatewayManagerConfigServiceImpl implements GatewayManagerConfigService {

    private static final String DOC_ADMIN_KEY = "gateway_doc_admin_user";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> listDocAdminUsers() {
        Set<String> members = this.stringRedisTemplate.opsForSet().members(DOC_ADMIN_KEY);
        if (members == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(members);
    }

    @Override
    public boolean addDocAdminUser(String empId) {
        this.stringRedisTemplate.opsForSet().add(DOC_ADMIN_KEY, empId);
        return true;
    }

    @Override
    public boolean removeDocAdminUser(String empId) {
        this.stringRedisTemplate.opsForSet().remove(DOC_ADMIN_KEY, empId);
        return true;
    }

    @Override
    public boolean isDocAdminUser(String empId) {
        return listDocAdminUsers().contains(empId);
    }
}
