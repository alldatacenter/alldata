package org.dromara.cloudeon.utils;

import lombok.Data;

import java.io.Serializable;
@Data
public class ExecResult implements Serializable {

    private boolean execResult = false;

    private String execOut;

    private String execErrOut;


}
