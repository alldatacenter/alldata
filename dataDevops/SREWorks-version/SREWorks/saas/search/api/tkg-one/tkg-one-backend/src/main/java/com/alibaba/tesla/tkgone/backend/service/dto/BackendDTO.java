package com.alibaba.tesla.tkgone.backend.service.dto;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * @author xueyong.zxy
 */

@Builder
@Data
public class BackendDTO {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String name;

    private String type;

    private String host;

    private Long port;

    private String user;

    private String password;

//    public BackendDTO(BackendVO backendVO) {
//        BeanUtils.copyProperties(backendVO, this);
//    }
}
