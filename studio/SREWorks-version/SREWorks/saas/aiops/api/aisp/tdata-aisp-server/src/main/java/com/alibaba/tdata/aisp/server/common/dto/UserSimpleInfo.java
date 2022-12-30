package com.alibaba.tdata.aisp.server.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: UserSimpleInfo
 * @Author: dyj
 * @DATE: 2021-08-18
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleInfo implements Serializable {
    private static final long serialVersionUID = -255673820158326532L;

    /**
     * empId
     */
    private String empId;

    /**
     * nic name
     */
    private String nickNameCn;

    /**
     * 中文名字
     */
    private String lastName;

    public static UserSimpleInfo from(String userSimpleInfoStr){
        if (StringUtils.isEmpty(userSimpleInfoStr)){
            return new UserSimpleInfo();
        }
        return JSONObject.parseObject(userSimpleInfoStr, UserSimpleInfo.class);
    }

    /**
     * to list
     * @param owners
     * @return
     */
    public static List<UserSimpleInfo> fromJson(String owners) {
        if (StringUtils.isEmpty(owners)){
            return Collections.emptyList();
        }
        return JSONObject.parseArray(owners, UserSimpleInfo.class);

    }


    public static String to(UserSimpleInfo creatorUser) {
        return creatorUser==null?null: JSONObject.toJSONString(creatorUser);
    }

    public static String to(List<UserSimpleInfo> ownersList) {
        return ownersList==null?null: JSONObject.toJSONString(ownersList);
    }

    /**
     * 兼容只存empId的场景
     * @param user
     * @return
     */
    public static List<String> fromDefault(String... user){
        List<String> empIds = new ArrayList<>(user.length);
        for (String s : user) {
            if(StringUtils.isNotBlank(s)){
                if(StringUtils.isNumeric(s)){
                    empIds.add(s);
                }else {
                    if(s.startsWith("[")){
                        List<UserSimpleInfo> userInfos = JSONObject.parseArray(s, UserSimpleInfo.class);
                        userInfos.stream().forEach(userSimpleInfo -> empIds.add(userSimpleInfo.getEmpId()));
                    }else if(s.startsWith("{")){
                        UserSimpleInfo userInfo = JSONObject.parseObject(s, UserSimpleInfo.class);
                        empIds.add(userInfo.getEmpId());
                    }else {
                        empIds.addAll(Arrays.asList(s.split(",")));
                    }

                }
            }
        }
        return new ArrayList<>(new HashSet<>(empIds));
    }
}
