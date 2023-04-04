package datart.core.mappers.ext;

import datart.core.entity.UserSettings;
import datart.core.mappers.UserSettingsMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserSettingsMapperExt extends UserSettingsMapper {

    @Select({
            "SELECT * FROM user_settings t WHERE t.user_id = #{userId}"
    })
    List<UserSettings> selectByUser(String userId);

    @Delete({
            "DELETE FROM user_settings WHERE user_id = #{userId}"
    })
    int deleteByUser(String userId);

}
