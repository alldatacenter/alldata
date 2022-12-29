package datart.core.mappers;

import datart.core.entity.RelVariableSubject;
import datart.core.mappers.ext.CRUDMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;

public interface RelVariableSubjectMapper extends CRUDMapper {
    @Delete({
        "delete from rel_variable_subject",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into rel_variable_subject (id, variable_id, ",
        "subject_id, subject_type, ",
        "`value`, create_time, ",
        "use_default_value, create_by, ",
        "update_time, update_by)",
        "values (#{id,jdbcType=VARCHAR}, #{variableId,jdbcType=VARCHAR}, ",
        "#{subjectId,jdbcType=VARCHAR}, #{subjectType,jdbcType=VARCHAR}, ",
        "#{value,jdbcType=VARCHAR}, #{createTime,jdbcType=TIMESTAMP}, ",
        "#{useDefaultValue,jdbcType=TINYINT}, #{createBy,jdbcType=VARCHAR}, ",
        "#{updateTime,jdbcType=TIMESTAMP}, #{updateBy,jdbcType=VARCHAR})"
    })
    int insert(RelVariableSubject record);

    @InsertProvider(type=RelVariableSubjectSqlProvider.class, method="insertSelective")
    int insertSelective(RelVariableSubject record);

    @Select({
        "select",
        "id, variable_id, subject_id, subject_type, `value`, create_time, use_default_value, ",
        "create_by, update_time, update_by",
        "from rel_variable_subject",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="variable_id", property="variableId", jdbcType=JdbcType.VARCHAR),
        @Result(column="subject_id", property="subjectId", jdbcType=JdbcType.VARCHAR),
        @Result(column="subject_type", property="subjectType", jdbcType=JdbcType.VARCHAR),
        @Result(column="value", property="value", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="use_default_value", property="useDefaultValue", jdbcType=JdbcType.TINYINT),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR)
    })
    RelVariableSubject selectByPrimaryKey(String id);

    @UpdateProvider(type=RelVariableSubjectSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(RelVariableSubject record);

    @Update({
        "update rel_variable_subject",
        "set variable_id = #{variableId,jdbcType=VARCHAR},",
          "subject_id = #{subjectId,jdbcType=VARCHAR},",
          "subject_type = #{subjectType,jdbcType=VARCHAR},",
          "`value` = #{value,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "use_default_value = #{useDefaultValue,jdbcType=TINYINT},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(RelVariableSubject record);
}