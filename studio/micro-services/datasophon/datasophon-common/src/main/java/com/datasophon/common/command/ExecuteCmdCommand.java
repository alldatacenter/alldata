package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ExecuteCmdCommand implements Serializable {

    private static final long serialVersionUID = 8665156195475027337L;

    private List<String> commands;
}
