package datart.server.config;

import datart.core.entity.BaseEntity;
import datart.core.log.AccessType;
import datart.security.base.ResourceType;
import datart.server.service.AsyncAccessLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AccessLogAdvice {

    private final AsyncAccessLogService logService;

    public AccessLogAdvice(AsyncAccessLogService logService) {
        this.logService = logService;
    }

    @Before(value = "execution(* datart.core.mappers..*.selectByPrimaryKey(java.lang.String)) && args(id)")
    public void selectByPrimaryKey(JoinPoint jp, String id) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        log(AccessType.READ, id, signature.getReturnType());
    }

    @Before(value = "execution(* datart.core.mappers..*.deleteByPrimaryKey(java.lang.String)) && args(id)")
    public void deleteByPrimaryKey(JoinPoint jp, String id) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        String typeName = signature.getDeclaringTypeName();
        log(AccessType.DELETE, id, typeName.replace("Mapper", "").replace("Ext", ""));
    }

    @Before(value = "execution(* datart.core.mappers..*.insert(datart.core.entity.BaseEntity)) && args(entity)")
    public void insert(JoinPoint jp, BaseEntity entity) {
        log(AccessType.CREATE, entity.getId(), entity.getClass());
    }

    @Before(value = "execution(* datart.core.mappers..*.updateByPrimaryKey*(datart.core.entity.BaseEntity)) && args(entity)")
    public void updateByPrimaryKey(JoinPoint jp, BaseEntity entity) {
        log(AccessType.UPDATE, entity.getId(), entity.getClass());
    }

    private void log(AccessType accessType, String id, Class<?> clz) {
        try {
            logService.log(accessType, getResourceType(clz), id);
        } catch (Exception ignored) {
        }
    }

    private void log(AccessType accessType, String id, String name) {
        try {
//            logService.log(accessType, ResourceType.valueOf(name.toUpperCase()), id);
        } catch (Exception ignored) {
        }
    }

    private ResourceType getResourceType(Class<?> clz) {
        try {
            return ResourceType.valueOf(clz.getSimpleName().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

}
