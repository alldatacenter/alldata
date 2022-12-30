package datart.core.mappers.ext;

import datart.core.entity.Download;
import datart.core.mappers.DownloadMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DownloadMapperExt extends DownloadMapper {
    @Select({
            "SELECT " +
                    " * " +
                    "FROM " +
                    " download " +
                    "WHERE" +
                    " create_by = #{userId} and create_time > (NOW() - INTERVAL 7 DAY)  order by create_time desc"
    })
    List<Download> selectByCreator(String userId);

}