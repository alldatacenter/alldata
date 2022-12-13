package com.platform.dts.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 构建json dto
 *
 * @author AllDataDC
 * @ClassName RdbmsWriteDto
 * @Version 2.0
 * @date 2022/11/11 17:15
 */
@Data
public class RdbmsWriterDto implements Serializable {

    private String preSql;

    private String postSql;
}
