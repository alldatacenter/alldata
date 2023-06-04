package datart.core.mappers;

import datart.core.entity.Link;
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

public interface LinkMapper extends CRUDMapper {
    @Delete({
        "delete from link",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into link (id, rel_type, ",
        "rel_id, url, expiration, ",
        "create_by, create_time)",
        "values (#{id,jdbcType=VARCHAR}, #{relType,jdbcType=VARCHAR}, ",
        "#{relId,jdbcType=VARCHAR}, #{url,jdbcType=VARCHAR}, #{expiration,jdbcType=TIMESTAMP}, ",
        "#{createBy,jdbcType=VARCHAR}, #{createTime,jdbcType=TIMESTAMP})"
    })
    int insert(Link record);

    @InsertProvider(type=LinkSqlProvider.class, method="insertSelective")
    int insertSelective(Link record);

    @Select({
        "select",
        "id, rel_type, rel_id, url, expiration, create_by, create_time",
        "from link",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="rel_type", property="relType", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_id", property="relId", jdbcType=JdbcType.VARCHAR),
        @Result(column="url", property="url", jdbcType=JdbcType.VARCHAR),
        @Result(column="expiration", property="expiration", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP)
    })
    Link selectByPrimaryKey(String id);

    @UpdateProvider(type=LinkSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Link record);

    @Update({
        "update link",
        "set rel_type = #{relType,jdbcType=VARCHAR},",
          "rel_id = #{relId,jdbcType=VARCHAR},",
          "url = #{url,jdbcType=VARCHAR},",
          "expiration = #{expiration,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Link record);
}