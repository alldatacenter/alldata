package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Data
public class FileOperateCommand implements Serializable {
    private TreeSet<String> lines;

    private String content;

    private String path;
}
