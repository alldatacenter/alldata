package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.constants.CheckNullObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @ClassName:AppObjectUtils
 * @author yangjie.dyj@alibaba-inc.com
 * @DATE: 2020-11-30
 * @Description:
 **/
@Slf4j
public class ObjectUtil {
    public static void checkNull(CheckNullObject ob) {
        if (ob.getCheckObject() == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                String.format("actionName=%s|%s is NULL!", ob.getActionName(), ob.getObjectName()));
        }

        if (ob.getFields() != null && ob.getFields().size() > 0) {
            Class<?> aClass = ob.getCheckObject().getClass();
            for (String attr : ob.getFields()) {
                Field declaredField;
                try {
                    declaredField = aClass.getDeclaredField(attr);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("actionName=%s|Object=%s|has no such field Exception: %s !", ob.getActionName(), ob.getObjectName(), attr),
                        noSuchFieldException.getStackTrace());
                }
                declaredField.setAccessible(true);
                Object o;
                try {
                    o = declaredField.get(ob.getCheckObject());
                } catch (IllegalAccessException illegalAccessException) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("actionName=%s|Object=%s|can not access to field:%s!", ob.getActionName(), ob.getObjectName(), attr),
                        illegalAccessException.getStackTrace());
                }
                if (o == null) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("actionName=%s|Object=%s|%s field is NULL!", ob.getActionName(), ob.getObjectName(), attr));
                }
            }
        }
    }

    public static void checkNullList(List<CheckNullObject> objectList){
        for (CheckNullObject checkNullObject : objectList) {
            checkNull(checkNullObject);
        }
    }
}
