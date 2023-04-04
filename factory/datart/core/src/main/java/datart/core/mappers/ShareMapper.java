package datart.core.mappers;

import datart.core.entity.Share;
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

public interface ShareMapper extends CRUDMapper {
    @Delete({
        "delete from share",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into share (id, org_id, ",
        "viz_type, viz_id, ",
        "authentication_mode, row_permission_by, ",
        "authentication_code, expiry_date, ",
        "create_by, create_time, ",
        "roles)",
        "values (#{id,jdbcType=VARCHAR}, #{orgId,jdbcType=VARCHAR}, ",
        "#{vizType,jdbcType=VARCHAR}, #{vizId,jdbcType=VARCHAR}, ",
        "#{authenticationMode,jdbcType=VARCHAR}, #{rowPermissionBy,jdbcType=VARCHAR}, ",
        "#{authenticationCode,jdbcType=VARCHAR}, #{expiryDate,jdbcType=TIMESTAMP}, ",
        "#{createBy,jdbcType=VARCHAR}, #{createTime,jdbcType=TIMESTAMP}, ",
        "#{roles,jdbcType=LONGVARCHAR})"
    })
    int insert(Share record);

    @InsertProvider(type=ShareSqlProvider.class, method="insertSelective")
    int insertSelective(Share record);

    @Select({
        "select",
        "id, org_id, viz_type, viz_id, authentication_mode, row_permission_by, authentication_code, ",
        "expiry_date, create_by, create_time, roles",
        "from share",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="viz_type", property="vizType", jdbcType=JdbcType.VARCHAR),
        @Result(column="viz_id", property="vizId", jdbcType=JdbcType.VARCHAR),
        @Result(column="authentication_mode", property="authenticationMode", jdbcType=JdbcType.VARCHAR),
        @Result(column="row_permission_by", property="rowPermissionBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="authentication_code", property="authenticationCode", jdbcType=JdbcType.VARCHAR),
        @Result(column="expiry_date", property="expiryDate", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="roles", property="roles", jdbcType=JdbcType.LONGVARCHAR)
    })
    Share selectByPrimaryKey(String id);

    @UpdateProvider(type=ShareSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Share record);

    @Update({
        "update share",
        "set org_id = #{orgId,jdbcType=VARCHAR},",
          "viz_type = #{vizType,jdbcType=VARCHAR},",
          "viz_id = #{vizId,jdbcType=VARCHAR},",
          "authentication_mode = #{authenticationMode,jdbcType=VARCHAR},",
          "row_permission_by = #{rowPermissionBy,jdbcType=VARCHAR},",
          "authentication_code = #{authenticationCode,jdbcType=VARCHAR},",
          "expiry_date = #{expiryDate,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "roles = #{roles,jdbcType=LONGVARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKeyWithBLOBs(Share record);

    @Update({
        "update share",
        "set org_id = #{orgId,jdbcType=VARCHAR},",
          "viz_type = #{vizType,jdbcType=VARCHAR},",
          "viz_id = #{vizId,jdbcType=VARCHAR},",
          "authentication_mode = #{authenticationMode,jdbcType=VARCHAR},",
          "roles = #{roles,jdbcType=VARCHAR},",
          "row_permission_by = #{rowPermissionBy,jdbcType=VARCHAR},",
          "authentication_code = #{authenticationCode,jdbcType=VARCHAR},",
          "expiry_date = #{expiryDate,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Share record);
}
