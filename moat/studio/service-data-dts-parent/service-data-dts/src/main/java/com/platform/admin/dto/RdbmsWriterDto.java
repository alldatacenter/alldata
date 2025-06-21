package com.platform.admin.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 构建json dto
 *
 * @author AllDataDC
 * @ClassName RdbmsWriteDto
 * @date 2022/01/11 17:15
 */
@Data
public class RdbmsWriterDto implements Serializable {

    private String preSql;

    private String postSql;
}
