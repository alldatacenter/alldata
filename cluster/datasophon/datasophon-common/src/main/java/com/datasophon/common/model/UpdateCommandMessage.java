package com.datasophon.common.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCommandMessage implements Serializable {

    private String commandId;

}
