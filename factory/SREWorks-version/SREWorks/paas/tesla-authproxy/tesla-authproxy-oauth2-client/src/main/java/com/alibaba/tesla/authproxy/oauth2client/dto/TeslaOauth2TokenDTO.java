package com.alibaba.tesla.authproxy.oauth2client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cdx
 * @date 2020/1/7 14:48
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeslaOauth2TokenDTO {
    private String tokenType;
    private String token;
    private Long tokenExpiration;
    private Boolean tokenIsExpired;
    private Long tokenExpiresIn;
    private String refreshToken;
    private Long refreshTokenExpiration;

}
