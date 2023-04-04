package datart.server.service;

import com.google.common.base.CaseFormat;
import datart.core.base.consts.Const;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import datart.core.common.UUIDGenerator;
import datart.core.entity.BaseEntity;
import datart.core.entity.User;
import datart.core.mappers.ext.CRUDMapper;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.security.base.ResourceType;
import datart.server.base.params.BaseCreateParam;
import datart.server.base.params.BaseUpdateParam;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;

public interface BaseCRUDService<E extends BaseEntity, M extends CRUDMapper> {

    @Transactional
    default E create(BaseCreateParam createParam) {
        E instance = getEntityInstance();
        BeanUtils.copyProperties(createParam, instance);
        // check create permission
        requirePermission(instance, Const.CREATE);

        instance.setCreateBy(getCurrentUser().getId());
        instance.setCreateTime(new Date());
        instance.setId(UUIDGenerator.generate());
        try {
            Method setStatus = instance.getClass().getDeclaredMethod("setStatus", Byte.class);
            setStatus.invoke(instance, Const.DATA_STATUS_ACTIVE);
        } catch (Exception ignored) {
        }
        getDefaultMapper().insert(instance);
        return instance;
    }

    default E retrieve(String id) {
        return retrieve(id, true);
    }

    default E retrieve(String id, boolean checkPermission) {
        E e = (E) getDefaultMapper().selectByPrimaryKey(id);
        if (e == null) {
            notFoundException();
        }
        if (checkPermission) {
            requirePermission(e, Const.READ);
        }
        return e;
    }

    @Transactional
    default boolean update(BaseUpdateParam updateParam) {
        E retrieve = retrieve(updateParam.getId());
        requirePermission(retrieve, Const.MANAGE);
        BeanUtils.copyProperties(updateParam, retrieve);
        retrieve.setUpdateBy(getCurrentUser().getId());
        retrieve.setUpdateTime(new Date());
        getDefaultMapper().updateByPrimaryKey(retrieve);

        return true;
    }

    @Transactional
    default boolean delete(String id, boolean archive) {
        return delete(id, archive, true);
    }

    @Transactional
    default boolean delete(String id, boolean archive, boolean safeDelete) {
        if (safeDelete && !safeDelete(id)) {
            Exceptions.tr(BaseException.class, "message.delete.error.relation");
        }
        return archive ? archive(id) : delete(id);
    }

    @Transactional
    default boolean delete(String id) {
        E retrieve = retrieve(id);
        requirePermission(retrieve, Const.MANAGE);
        deleteStaticFiles(retrieve);
        deletePermissions(retrieve);
        deleteReference(retrieve);
        return getDefaultMapper().deleteByPrimaryKey(id) == 1;
    }

    @Transactional
    default boolean archive(String id) {
        E entity = retrieve(id);
        requirePermission(entity, Const.MANAGE);

        E instance = getEntityInstance();
        instance.setId(id);
        try {
            // rename it
            String name = (String) instance.getClass().getDeclaredMethod("getName").invoke(entity);
            name = name + "." + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
            Method setName = instance.getClass().getDeclaredMethod("setName", String.class);
            setName.invoke(instance, name);

            // set status
            Method setStatus = instance.getClass().getDeclaredMethod("setStatus", Byte.class);
            setStatus.invoke(instance, Const.DATA_STATUS_ARCHIVED);
            getDefaultMapper().updateByPrimaryKeySelective(instance);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Exceptions.msg(String.format("Object %s has no property status", getEntityClz().getSimpleName()));
        }
        return true;
    }

    @Transactional
    default boolean unarchive(String id) {
        E entity = retrieve(id);
        requirePermission(entity, Const.MANAGE);
        E instance = getEntityInstance();
        instance.setId(id);
        try {
            Method setStatus = entity.getClass().getMethod("setStatus", Byte.class);
            setStatus.invoke(instance, Const.DATA_STATUS_ACTIVE);
            getDefaultMapper().updateByPrimaryKeySelective(instance);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            Exceptions.msg(String.format("Object %s has no property status", getEntityClz().getSimpleName()));
        }

        return true;
    }


    default void requireExists(String id) {
        if (!getDefaultMapper().exists(id)) {
            notFoundException();
        }
    }

    default void deleteStaticFiles(E e) {

    }

    default void deletePermissions(E e) {
        getRRRMapper().deleteByResourceId(e.getId());
    }

    default void deleteReference(E e) {

    }

    default void grantDefaultPermission(E entity) {

    }

    default boolean checkUnique(BaseEntity entity) {
        if (!getDefaultMapper().checkUnique(entity)) {
            Exceptions.tr(BaseException.class, "error.param.exists.name");
        }
        return true;
    }

    default E getEntityInstance() {
        try {
            return ((Class<E>) getParameterizedType().getActualTypeArguments()[0]).newInstance();
        } catch (Exception e) {
            Exceptions.msg("entity instant exception");
        }
        return null;
    }


    default Class<E> getEntityClz() {
        return ((Class<E>) getParameterizedType().getActualTypeArguments()[0]);
    }

    /**
     * 获取当前实例Service所对应的Mapper
     *
     * @return mapper 实例
     */
    default M getDefaultMapper() {
        return Application.getContext().getBean(defaultMapperClass());
    }

    default Class<M> defaultMapperClass() {
        return (Class<M>) getParameterizedType().getActualTypeArguments()[1];
    }

    default String getResourcePropertyName() {
        String simpleName = getEntityClz().getSimpleName();
        return String.format("resource.%s", CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, simpleName));
    }

    default void notFoundException() {
        Exceptions.notFound(getResourcePropertyName());
    }


    /**
     * 获取当前实例的CRUDService泛型参数：实体类型和Mapper类型。
     * 用于CRUD操作
     *
     * @return 实体类型和Mapper类型
     */
    default ParameterizedType getParameterizedType() {
        try {
            Type[] genericInterfaces = getClass().getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof Class) {
                    for (Type anInterface : ((Class<?>) genericInterface).getGenericInterfaces()) {
                        if (anInterface instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) anInterface;
                            return parameterizedType;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            Exceptions.msg("entity instant exception");
        }
        return null;
    }

    default boolean safeDelete(String id) {
        return true;
    }

    default ResourceType getResourceType() {
        try {
            return ResourceType.valueOf(getEntityClz().getSimpleName().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }

    User getCurrentUser();

    void requirePermission(E entity, int permission);

    RelRoleResourceMapperExt getRRRMapper();

}