/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.mappers.ext;

import com.google.common.base.CaseFormat;
import datart.core.common.ReflectUtils;
import datart.core.entity.BaseEntity;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

public interface CRUDMapper extends BaseMapper {

    int deleteByPrimaryKey(String id);

    int insert(BaseEntity record);

    BaseEntity selectByPrimaryKey(String id);

    int updateByPrimaryKey(BaseEntity record);

    int updateByPrimaryKeySelective(BaseEntity record);

    default boolean exists(String id) {
        SQL sql = new SQL();
        sql.SELECT("COUNT(*)").FROM(getTableName()).WHERE("`id` = ?");
        Long count = executeQuery(sql.toString(), Long.class, id);
        return count > 0;
    }

    default boolean checkUnique(BaseEntity entity) {
        SQL sql = new SQL();
        sql.SELECT("COUNT(*)").FROM(format(entity.getClass().getSimpleName()));
        Map<Field, Object> fields = ReflectUtils.getNotNullFields(entity);
        if (CollectionUtils.isEmpty(fields)) {
            return false;
        }
        ArrayList<Object> args = new ArrayList<>();
        for (Map.Entry<Field, Object> entry : fields.entrySet()) {
            sql.WHERE(String.format("`%s` = ?", format(entry.getKey().getName())));
            args.add(entry.getValue());
        }
        long count = executeQuery(sql.toString(), Long.class, args.toArray());
        return count == 0;
    }

    default int deleteByPrimaryKeys(Class<?> c, Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        SQL sql = new SQL();
        StringJoiner stringJoiner = new StringJoiner(",", "(", ")");
        for (String id : ids) {
            stringJoiner.add("?");
        }
        sql.DELETE_FROM(format(c.getSimpleName()))
                .WHERE("`id` IN " + stringJoiner);
        return executeDelete(sql.toString(), ids.toArray());
    }

    default String getTableName() {
        Type mapperExt = this.getClass().getGenericInterfaces()[0];
        String[] split = mapperExt.getTypeName().split("\\.");
        String entityName = split[split.length - 1].replace("MapperExt", "");
        return format(entityName);
    }

    default String format(String field) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field);
    }

}