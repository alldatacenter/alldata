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

import datart.core.entity.SourceSchemas;
import datart.core.mappers.SourceSchemasMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

@Mapper
public interface SourceSchemasMapperExt extends SourceSchemasMapper {

    @Select("SELECT * FROM source_schemas where source_id = #{sourceId}")
    SourceSchemas selectBySource(String sourceId);

    @Select("SELECT update_time FROM source_schemas where source_id = #{sourceId}")
    Date selectUpdateDateBySource(String sourceId);

    @Delete("DELETE FROM source_schemas where source_id = #{sourceId}")
    int deleteBySource(String sourceId);

}
