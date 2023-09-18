package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetLogCommand implements Serializable {

    private String logFile;

    private String decompressPackageName;

}
