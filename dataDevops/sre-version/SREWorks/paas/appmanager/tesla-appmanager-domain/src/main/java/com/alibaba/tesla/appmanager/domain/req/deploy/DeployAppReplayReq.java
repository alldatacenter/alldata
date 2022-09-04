package com.alibaba.tesla.appmanager.domain.req.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 重放指定部署单
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppReplayReq implements Serializable {

    private static final long serialVersionUID = 4655806905534432716L;

    /**
     * 部署单 ID
     */
    private Long deployAppId;
}
