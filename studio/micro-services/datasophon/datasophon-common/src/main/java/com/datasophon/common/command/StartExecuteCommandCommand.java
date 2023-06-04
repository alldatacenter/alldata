package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class StartExecuteCommandCommand implements Serializable {

    private List<String> commandIds;

    private Integer clusterId;

    private CommandType commandType;

    public StartExecuteCommandCommand(List<String> list,Integer clusterId,CommandType commandType) {
        this.commandIds = list;
        this.clusterId = clusterId;
        this.commandType = commandType;
    }
}
