package org.dromara.cloudeon.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Component
public class CloudeonConfigProp {
    @Value("${cloudeon.stack.load.path}")
    private String stackLoadPath;
    @Value("${cloudeon.remote.script.path}")
    private String remoteScriptPath;
    @Value("${cloudeon.task.log}")
    private String taskLog;
    @Value("${cloudeon.work.home}")
    private String workHome;

}

