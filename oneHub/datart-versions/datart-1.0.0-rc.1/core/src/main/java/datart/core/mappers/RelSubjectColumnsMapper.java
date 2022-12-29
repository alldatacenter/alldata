package datart.core.mappers;

import datart.core.entity.RelSubjectColumns;
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

public interface RelSubjectColumnsMapper extends CRUDMapper {
    @Delete({
        "delete from rel_subject_columns",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into rel_subject_columns (id, view_id, ",
        "subject_id, subject_type, ",
        "create_by, create_time, ",
        "update_by, update_time, ",
        "column_permission)",
        "values (#{id,jdbcType=VARCHAR}, #{viewId,jdbcType=VARCHAR}, ",
        "#{subjectId,jdbcType=VARCHAR}, #{subjectType,jdbcType=VARCHAR}, ",
        "#{createBy,jdbcType=VARCHAR}, #{createTime,jdbcType=TIMESTAMP}, ",
        "#{updateBy,jdbcType=VARCHAR}, #{updateTime,jdbcType=TIMESTAMP}, ",
        "#{columnPermission,jdbcType=LONGVARCHAR})"
    })
    int insert(RelSubjectColumns record);

    @InsertProvider(type=RelSubjectColumnsSqlProvider.class, method="insertSelective")
    int insertSelective(RelSubjectColumns record);

    @Select({
        "select",
        "id, view_id, subject_id, subject_type, create_by, create_time, update_by, update_time, ",
        "column_permission",
        "from rel_subject_columns",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="view_id", property="viewId", jdbcType=JdbcType.VARCHAR),
        @Result(column="subject_id", property="subjectId", jdbcType=JdbcType.VARCHAR),
        @Result(column="subject_type", property="subjectType", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="column_permission", property="columnPermission", jdbcType=JdbcType.LONGVARCHAR)
    })
    RelSubjectColumns selectByPrimaryKey(String id);

    @UpdateProvider(type=RelSubjectColumnsSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(RelSubjectColumns record);

    @Update({
        "update rel_subject_columns",
        "set view_id = #{viewId,jdbcType=VARCHAR},",
          "subject_id = #{subjectId,jdbcType=VARCHAR},",
          "subject_type = #{subjectType,jdbcType=VARCHAR},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP},",
          "column_permission = #{columnPermission,jdbcType=LONGVARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKeyWithBLOBs(RelSubjectColumns record);

    @Update({
        "update rel_subject_columns",
        "set view_id = #{viewId,jdbcType=VARCHAR},",
          "subject_id = #{subjectId,jdbcType=VARCHAR},",
          "subject_type = #{subjectType,jdbcType=VARCHAR},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(RelSubjectColumns record);
}