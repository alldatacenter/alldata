package com.alibaba.sreworks.other.server.params;

import lombok.Data;

@Data
public class SreworksFileCreateRequestParam {

    private String category;

    private String name;

    private String alias;

    private String fileId;

    private String type;

    private String description;

    public String fileId() {
        String sreworksFilePrefix = System.getenv("SREWORKS_FILE_PREFIX");
        if(fileId.startsWith(sreworksFilePrefix)) {
            String[] words = fileId.split("/");
            return words[words.length - 1];
        }else{
            return fileId;
        }
    }

}
