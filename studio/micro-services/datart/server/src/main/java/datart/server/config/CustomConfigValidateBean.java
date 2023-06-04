package datart.server.config;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/**
 * 校验配置文件中的key规则
 */
@Data
@Validated
public class CustomConfigValidateBean {

    @NotBlank
    @JSONField(name = "datasource.ip")
    private String datasourceIp;

    @NotBlank
    @JSONField(name = "datasource.port")
    private String datasourcePort;

    @NotBlank
    @JSONField(name = "datasource.database")
    private String datasourceDatabase;

    @NotBlank
    @JSONField(name = "datasource.username")
    private String datasourceUsername;

    @NotBlank
    @JSONField(name = "datasource.password")
    private String datasourcePassword;

}
