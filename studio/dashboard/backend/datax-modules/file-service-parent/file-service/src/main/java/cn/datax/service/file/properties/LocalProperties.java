package cn.datax.service.file.properties;

import lombok.Data;

@Data
public class LocalProperties {
    /**
     * 上传文件存储在本地的根路径
     */
    private String path;
    /**
     * url访问前缀
     */
    private String prefix;
}
