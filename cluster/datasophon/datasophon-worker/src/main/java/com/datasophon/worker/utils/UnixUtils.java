package com.datasophon.worker.utils;

import com.datasophon.common.Constants;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.actor.InstallServiceActor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class UnixUtils {
    private static Long TIMEOUT = 60L;

    private static final Logger logger = LoggerFactory.getLogger(UnixUtils.class);

    public static ExecResult createUnixUser(String username , String mainGroup , String otherGroups){
        ArrayList<String> commands = new ArrayList<>();
        if(isUserExists(username)){
            commands.add("usermod");
        }else{
            commands.add("useradd");
        }
        commands.add(username);
        if(StringUtils.isNotBlank(mainGroup)){
            commands.add("-g");
            commands.add(mainGroup);
        }
        if(StringUtils.isNotBlank(otherGroups)){
            commands.add("-G");
            commands.add(otherGroups);
        }
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH,commands,TIMEOUT);
    }

    public static ExecResult delUnixUser(String username){
        ArrayList<String> commands = new ArrayList<>();
        commands.add("userdel");
        commands.add("-r");
        commands.add(username);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH,commands,TIMEOUT);
    }

    public static boolean isUserExists(String username){
        ArrayList<String> commands = new ArrayList<>();
        commands.add("id");
        commands.add(username);
        ExecResult execResult = ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, TIMEOUT);
        return execResult.getExecResult();
    }

    public static ExecResult createUnixGroup(String groupName ){
        if (isGroupExists(groupName)) {
            ExecResult execResult = new ExecResult();
            execResult.setExecResult(true);
            return execResult;
        }
        ArrayList<String> commands = new ArrayList<>();
        commands.add("groupadd");
        commands.add(groupName);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH,commands,TIMEOUT);
    }

    public static ExecResult delUnixGroup(String groupName ){
        ArrayList<String> commands = new ArrayList<>();
        commands.add("groupdel");
        commands.add(groupName);
        return ShellUtils.execWithStatus(Constants.INSTALL_PATH,commands,TIMEOUT);
    }

    public static boolean isGroupExists(String groupName){
        ExecResult execResult = ShellUtils.exceShell("egrep \""+groupName+"\" /etc/group >& /dev/null");
        return execResult.getExecResult();
    }


}
