package com.webank.wedatasphere.streamis.project.server.utils;

import com.webank.wedatasphere.streamis.project.server.entity.StreamisProjectPrivilege;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class StreamisProjectPrivilegeUtils {
    public static List<StreamisProjectPrivilege> createStreamisProjectPrivilege(Long projectId, List<String> users, int privilege){
        List<StreamisProjectPrivilege> retList = new ArrayList<>();
        if(CollectionUtils.isEmpty(users)){
            return retList;
        }
        users.forEach(user->{
            StreamisProjectPrivilege streamisProjectPrivilege = new StreamisProjectPrivilege(projectId, user, privilege);
            retList.add(streamisProjectPrivilege);
        });
        return retList;
    }
}
