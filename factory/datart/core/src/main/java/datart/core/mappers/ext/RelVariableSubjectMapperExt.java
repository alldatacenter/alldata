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

import datart.core.entity.RelVariableSubject;
import datart.core.mappers.RelVariableSubjectMapper;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RelVariableSubjectMapperExt extends RelVariableSubjectMapper {

    @Select({
            "SELECT * FROM rel_variable_subject rvs JOIN variable v ON rvs.variable_id=v.id AND v.org_id = #{orgId} AND rvs.subject_type =#{subjectType} AND subject_id=#{subjectId}"
    })
    List<RelVariableSubject> selectBySubject(String orgId, String subjectType, String subjectId);

    @Select({
            "SELECT * FROM rel_variable_subject WHERE rvs.subject_type ='USER' AND subject_id=#{subjectId}"
    })
    List<RelVariableSubject> selectByUser(String userId);

    @Select({
            "<script>",
            "SELECT * FROM rel_variable_subject WHERE subject_id IN",
            "<foreach collection='subjectIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ",
            " AND variable_id IN ",
            "<foreach collection='varIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ",
            "</script>",
    })
    List<RelVariableSubject> selectByVarAndSubject(Collection<String> varIds, Collection<String> subjectIds);

    @Select({
            "<script>",
            "SELECT * FROM rel_variable_subject rvs JOIN variable v ON v.org_id=#{orgId} AND v.view_id is NULL AND rvs.variable_id = v.id AND rvs.subject_id IN",
            "<foreach collection='subjectIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    List<RelVariableSubject> selectBySubjects(String orgId, Collection<String> subjectIds);

    @Select({
            "SELECT * FROM rel_variable_subject WHERE variable_id = #{variableId}"
    })
    List<RelVariableSubject> selectVariableRels(String variableId);

    @Select({
            "<script>",
            "SELECT * FROM rel_variable_subject WHERE id IN ",
            "<foreach collection='relIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    List<RelVariableSubject> selectByIds(Collection<String> relIds);

    @Select({
            "SELECT * FROM rel_variable_subject WHERE variable_id IN ",
            "( SELECT id FROM variable WHERE view_id = #{viewId})",
    })
    List<RelVariableSubject> selectByView(String viewId);

    @Update({
            "<script>",
            "<foreach collection='elements' item='record' index='index' separator=';'>",
            "update rel_variable_subject",
            "set variable_id = #{variableId,jdbcType=VARCHAR},",
            "subject_id = #{subjectId,jdbcType=VARCHAR},",
            "subject_type = #{subjectType,jdbcType=VARCHAR},",
            "`value` = #{value,jdbcType=VARCHAR},",
            "create_time = #{createTime,jdbcType=TIMESTAMP},",
            "create_by = #{createBy,jdbcType=VARCHAR},",
            "update_time = #{updateTime,jdbcType=TIMESTAMP},",
            "update_by = #{updateBy,jdbcType=VARCHAR}",
            "where id = #{id,jdbcType=VARCHAR}",
            "</foreach>",
            "</script>",
    })
    int batchUpdate(List<RelVariableSubject> elements);

    @Insert({
            "<script>",
            "insert into rel_variable_subject (id, variable_id, ",
            "subject_id, subject_type, ",
            "`value`, create_time, use_default_value,",
            "create_by, update_time,",
            "update_by) VALUES ",
            "<foreach collection='elements' item='record' index='index' separator=','>",
            " <trim prefix='(' suffix=')' suffixOverrides=','>",
            " #{record.id,jdbcType=VARCHAR}, #{record.variableId,jdbcType=VARCHAR}, ",
            "#{record.subjectId,jdbcType=VARCHAR}, #{record.subjectType,jdbcType=VARCHAR}, ",
            "#{record.value,jdbcType=VARCHAR}, #{record.createTime,jdbcType=TIMESTAMP}, ",
            "#{record.useDefaultValue,jdbcType=TINYINT}, #{record.createBy,jdbcType=VARCHAR}, ",
            "#{record.updateTime,jdbcType=TIMESTAMP}, #{record.updateBy,jdbcType=VARCHAR} ",
            "</trim>",
            "</foreach>",
            "</script>",
    })
    int batchInsert(List<RelVariableSubject> elements);

    @Delete({
            "<script>",
            "DELETE FROM rel_variable_subject where variable_id IN ",
            "<foreach collection='varIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    int deleteByVariables(Collection<String> varIds);
}
