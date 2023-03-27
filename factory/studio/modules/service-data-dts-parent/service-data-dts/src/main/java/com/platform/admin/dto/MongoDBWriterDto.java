package com.platform.admin.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 构建mongodb write dto
 *
 * @author AllDataDC
 * @ClassName mongodb write dto
 * @Version 2.1.1
 * @date 2022/03/14 07:15
 */
@Data
public class MongoDBWriterDto implements Serializable {

    private UpsertInfo upsertInfo;

}
