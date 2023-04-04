package datart.core.mappers;

import datart.core.entity.Download;
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

public interface DownloadMapper extends CRUDMapper {
    @Delete({
        "delete from download",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into download (id, `name`, ",
        "`path`, last_download_time, ",
        "create_time, create_by, ",
        "`status`)",
        "values (#{id,jdbcType=VARCHAR}, #{name,jdbcType=VARCHAR}, ",
        "#{path,jdbcType=VARCHAR}, #{lastDownloadTime,jdbcType=TIMESTAMP}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{createBy,jdbcType=VARCHAR}, ",
        "#{status,jdbcType=TINYINT})"
    })
    int insert(Download record);

    @InsertProvider(type=DownloadSqlProvider.class, method="insertSelective")
    int insertSelective(Download record);

    @Select({
        "select",
        "id, `name`, `path`, last_download_time, create_time, create_by, `status`",
        "from download",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="path", property="path", jdbcType=JdbcType.VARCHAR),
        @Result(column="last_download_time", property="lastDownloadTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="status", property="status", jdbcType=JdbcType.TINYINT)
    })
    Download selectByPrimaryKey(String id);

    @UpdateProvider(type=DownloadSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Download record);

    @Update({
        "update download",
        "set `name` = #{name,jdbcType=VARCHAR},",
          "`path` = #{path,jdbcType=VARCHAR},",
          "last_download_time = #{lastDownloadTime,jdbcType=TIMESTAMP},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "`status` = #{status,jdbcType=TINYINT}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Download record);
}