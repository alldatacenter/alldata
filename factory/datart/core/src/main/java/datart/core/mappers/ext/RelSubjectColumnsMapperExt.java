package datart.core.mappers.ext;

import datart.core.entity.RelSubjectColumns;
import datart.core.mappers.RelSubjectColumnsMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RelSubjectColumnsMapperExt extends RelSubjectColumnsMapper {

    @Select({
            "SELECT ",
            "	rrv.* ",
            "FROM ",
            "	rel_subject_columns rrv ",
            "WHERE ",
            "	rrv.view_id = #{viewId}"
    })
    List<RelSubjectColumns> listByView(String viewId);

    @Select({
            "SELECT " +
                    "	*  " +
                    "FROM " +
                    "	rel_subject_columns rsc  " +
                    "WHERE " +
                    "	rsc.view_id = #{viewId}  " +
                    "	AND rsc.subject_id IN ( " +
                    "	SELECT " +
                    "		role_id  " +
                    "	FROM " +
                    "		rel_role_user rru  " +
                    "WHERE " +
                    "	rru.user_id = #{userId})"
    })
    List<RelSubjectColumns> listByUser(String viewId, String userId);

    @Select({
            "SELECT * FROM rel_subject_columns rsc WHERE rsc.subject_id=#{roleId}"
    })
    List<RelSubjectColumns> listByRole(String roleId);

    @Insert({
            "<script>",
            "INSERT INTO rel_subject_columns (id, view_id,subject_id, subject_type, create_by, create_time,update_by, update_time, column_permission)  VALUES ",
            "<foreach collection='relRoleViews' item='record' index='index' separator=','>",
            "<trim prefix='(' suffix=')' suffixOverrides=','>",
            "#{record.id,jdbcType=VARCHAR}, #{record.viewId,jdbcType=VARCHAR}, ",
            "#{record.subjectId,jdbcType=VARCHAR}, #{record.subjectType,jdbcType=VARCHAR}, ",
            "#{record.createBy,jdbcType=VARCHAR}, #{record.createTime,jdbcType=TIMESTAMP}, ",
            "#{record.updateBy,jdbcType=VARCHAR}, #{record.updateTime,jdbcType=TIMESTAMP}, ",
            "#{record.columnPermission,jdbcType=LONGVARCHAR}",
            "</trim>",
            "</foreach>",
            "</script>",
    })
    int batchInsert(List<RelSubjectColumns> relRoleViews);


    @Update({
            "<script>",
            "<foreach collection='relRoleViews' item='record' index='index' separator=';'>",
            "UPDATE rel_subject_columns SET",
            "view_id = #{viewId,jdbcType=VARCHAR},",
            "role_id = #{roleId,jdbcType=VARCHAR},",
            "columnPermission = #{permission,jdbcType=VARCHAR},",
            "create_by = #{createBy,jdbcType=VARCHAR},",
            "create_time = #{createTime,jdbcType=TIMESTAMP},",
            "update_by = #{updateBy,jdbcType=VARCHAR},",
            "update_time = #{updateTime,jdbcType=TIMESTAMP}",
            "WHERE id = #{record.id,jdbcType=VARCHAR}",
            "</foreach>",
            "</script>",
    })
    int batchUpdate(List<RelSubjectColumns> relRoleViews);

    @Delete({
            "<script>",
            "DELETE  FROM rel_subject_columns WHERE id IN " +
                    "<foreach collection='ids' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    int batchDelete(List<String> ids);

    @Delete({
            "DELETE FROM rel_subject_columns WHERE view_id = #{viewId};"
    })
    int deleteByView(String viewId);


}
