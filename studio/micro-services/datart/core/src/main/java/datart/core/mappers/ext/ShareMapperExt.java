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

import datart.core.entity.Share;
import datart.core.mappers.ShareMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShareMapperExt extends ShareMapper {

    @Select({"<script>",
            "SELECT * FROM `share` where viz_id = #{vizId} AND create_by &lt;&gt; 'SCHEDULER' ORDER BY create_time",
            "</script>"})
    List<Share> selectByViz(String vizId);

}
