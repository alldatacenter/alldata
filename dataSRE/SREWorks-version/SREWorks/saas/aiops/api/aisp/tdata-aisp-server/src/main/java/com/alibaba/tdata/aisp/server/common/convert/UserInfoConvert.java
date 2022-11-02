package com.alibaba.tdata.aisp.server.common.convert;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.tdata.aisp.server.common.dto.UserSimpleInfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @ClassName: UserInfoConvert
 * @Author: dyj
 * @DATE: 2021-11-23
 * @Description:
 **/
@Slf4j
@Component
public class UserInfoConvert {
    public String to(UserSimpleInfo userSimpleInfo){
        if(null == userSimpleInfo){
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (!StringUtils.isEmpty(userSimpleInfo.getNickNameCn())){
            builder.append(userSimpleInfo.getEmpId()).append(":").append(userSimpleInfo.getNickNameCn());
        } else {
            // 兼容无花名
            builder.append(userSimpleInfo.getEmpId()).append(":").append(userSimpleInfo.getLastName());
        }
        return builder.toString();
    }

    public String tos(List<UserSimpleInfo> userSimpleInfos) {
        if(CollectionUtils.isEmpty(userSimpleInfos)){
            return null;
        }
        List<String> empIds = userSimpleInfos.stream().map(this::to).collect(Collectors.toList());
        return String.join(",", empIds);
    }

    /**
     * creator 为 empId:nickNameCn 格式的字符串。
     * @param creator
     * @return
     */
    public UserSimpleInfo from(String creator) {
        if (StringUtils.isEmpty(creator)){
            return null;
        }
        int i = creator.indexOf(":");
        if (i>-1){
            String[] split = creator.split(":");
            UserSimpleInfo userInfo = new UserSimpleInfo();
            if (split.length>1){
                userInfo.setEmpId(split[0]);
                userInfo.setNickNameCn(split[1]);
            } else if (split.length==1){
                if (i==0){
                    userInfo.setNickNameCn(split[0]);
                } else {
                    userInfo.setEmpId(split[0]);
                }
            } else {
                return null;
            }
            return userInfo;
        } else {
            return null;
        }
    }

    public List<UserSimpleInfo> froms(List<String> empInfoList) {
        List<UserSimpleInfo> userSimpleInfoList= new LinkedList<>();
        if (CollectionUtils.isEmpty(empInfoList)){
            return userSimpleInfoList;
        }
        for (String s : empInfoList) {
            UserSimpleInfo userSimpleInfo = from(s);
            if (userSimpleInfo!=null){
                userSimpleInfoList.add(userSimpleInfo);
            }
        }
        return userSimpleInfoList;
    }
}
