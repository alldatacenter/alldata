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

import datart.core.entity.User;
import datart.core.mappers.UserMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;


@Mapper
public interface UserMapperExt extends UserMapper {

    @Select({
            "SELECT ",
            "	u.*  ",
            "FROM ",
            "	`user` u  ",
            "WHERE ",
            "	u.username = #{username} or ",
            "	u.email = #{username}"
    })
    User selectByNameOrEmail(@Param("username") String username);

    @Select({
            "SELECT ",
            "	COUNT(*)  ",
            "FROM ",
            "	`user` u  "
    })
    int selectUserCount();

    @Select({
            "SELECT " +
                    "	u.*  " +
                    "FROM " +
                    "	`user` u  " +
                    "WHERE " +
                    "	u.email = #{email}"
    })
    User selectByEmail(@Param("email") String username);

    @Select({"SELECT " +
            "	count( DISTINCT u.email )  " +
            "FROM " +
            "	`user` u  " +
            "WHERE " +
            "	u.email = #{email}"})
    long countEmail(@Param("email") String email);

    @Select({
            " select * ",
            " from `user` u",
            " where ",
            " lower(`username`) like #{keyword}",
            " or lower(`name`) like #{keyword}",
            " or lower(`email`) like #{keyword}",
    })
    List<User> searchUsers(@Param("keyword") String keyword);

    @Select({
            "SELECT " +
                    "	u.*  " +
                    "FROM " +
                    "	`user` u  " +
                    "WHERE " +
                    "	u.`username` = #{username}"
    })
    User selectByUsername(@Param("username") String username);

    @Update({
            "UPDATE `user` u " +
                    "SET u.active = 1  " +
                    "WHERE " +
                    "	u.active = 0  " +
                    "	AND u.id = #{userId}"
    })
    int updateToActiveById(String id);

}