package com.datasophon.common.exception;

import lombok.Data;

import java.io.Serializable;

@Data
public class AkkaRemoteException implements Serializable {

    private String hostCommandId;

    private String errMsg;

}
