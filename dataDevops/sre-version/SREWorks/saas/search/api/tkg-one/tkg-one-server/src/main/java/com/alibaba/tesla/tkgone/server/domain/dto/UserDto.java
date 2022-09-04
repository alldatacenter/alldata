package com.alibaba.tesla.tkgone.server.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yangjinghua
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown=true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto{

    private String account;
    private String empId;
    private String nickNameCn;
    private String firstName;
    private String lastName;
    private String aliww;
    private String tbww;
    private String primaryEmail;
    private String emailPrefix;
    private String depId;

}