package datart.server.service;

import datart.core.base.consts.Const;
import datart.core.base.exception.Exceptions;
import datart.core.entity.BaseEntity;
import datart.core.mappers.ext.CRUDMapper;
import datart.security.manager.DatartSecurityManager;
import datart.server.base.params.BaseCreateParam;
import datart.server.base.params.VizCreateParam;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public interface VizCRUDService<E extends BaseEntity, M extends CRUDMapper> extends BaseCRUDService<E, M> {

    @Override
    @Transactional
    default E create(BaseCreateParam createParam) {
        VizCreateParam vizCreateParam = (VizCreateParam) createParam;

        E instance = getEntityInstance();
        BeanUtils.copyProperties(createParam, instance);

        requirePermission(instance, Const.CREATE);

//        checkUnique(instance);

        E e = BaseCRUDService.super.create(vizCreateParam);
        getRoleService().grantPermission(vizCreateParam.getPermissions());

        return e;
    }

    default List<E> listArchived(String orgId) {
        M mapper = getDefaultMapper();
        try {
            Method listArchived = mapper.getClass().getDeclaredMethod("listArchived", String.class);
            List<E> eList = (List<E>) listArchived.invoke(mapper, orgId);
            if (CollectionUtils.isEmpty(eList)) {
                return eList;
            }
            return eList.stream()
                    .filter(e -> {
                        try {
                            requirePermission(e, Const.MANAGE);
                            return true;
                        } catch (Exception ex) {
                            return false;
                        }
                    }).collect(Collectors.toList());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Exceptions.msg(String.format("Object %s has no property listArchived", getEntityClz().getSimpleName()));
        }
        return null;
    }

    default boolean updateStatus(String id, byte status) {
        requireExists(id);
        E instance = getEntityInstance();

        instance.setId(id);
        instance.setUpdateBy(getSecurityManager().getCurrentUser().getId());
        instance.setUpdateTime(new Date());
        try {
            Method setStatus = instance.getClass().getDeclaredMethod("setStatus", Byte.class);
            setStatus.invoke(instance, status);
        } catch (Exception ignored) {
        }
        return getDefaultMapper().updateByPrimaryKeySelective(instance) == 1;
    }

    RoleService getRoleService();

    DatartSecurityManager getSecurityManager();


}
