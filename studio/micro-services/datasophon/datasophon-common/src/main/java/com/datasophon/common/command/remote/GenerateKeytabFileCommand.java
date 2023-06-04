package com.datasophon.common.command.remote;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerateKeytabFileCommand implements Serializable {

    private String principal;

    private String keytabName;

    private String hostname;
}
