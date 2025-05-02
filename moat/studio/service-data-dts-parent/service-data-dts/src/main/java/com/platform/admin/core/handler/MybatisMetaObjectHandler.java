package com.platform.admin.core.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 通用的字段填充，如createBy createDate这些字段的自动填充
 **/
@Component
@Slf4j
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        setFieldValByName("createDate", new Date(), metaObject);
        setFieldValByName("createBy", getCurrentUser(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateDate", new Date(), metaObject);
        setFieldValByName("updateBy", getCurrentUser(), metaObject);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }
}