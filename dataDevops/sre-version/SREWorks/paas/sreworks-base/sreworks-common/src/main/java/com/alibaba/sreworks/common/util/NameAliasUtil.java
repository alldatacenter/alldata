package com.alibaba.sreworks.common.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.sreworks.common.DTO.NameAlias;
import com.alibaba.sreworks.common.annotation.Alias;

/**
 * @author jinghua.yjh
 */
public class NameAliasUtil {

    public static <T> List<NameAlias> getNameAliasList(Class<T> clazz) {
        return getNameAliasList(clazz, false);
    }

    public static <T> List<NameAlias> getNameAliasList(Class<T> clazz, boolean includeAll) {
        List<NameAlias> nameAliasList = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            NameAlias nameAlias = NameAlias.builder()
                .name(field.getName())
                .alias(field.getName())
                .build();
            if (field.isAnnotationPresent(Alias.class)) {
                nameAlias.setAlias(field.getAnnotation(Alias.class).value());
            } else {
                if (!includeAll) {
                    continue;
                }
            }
            nameAliasList.add(nameAlias);
        }
        return nameAliasList;
    }

}
