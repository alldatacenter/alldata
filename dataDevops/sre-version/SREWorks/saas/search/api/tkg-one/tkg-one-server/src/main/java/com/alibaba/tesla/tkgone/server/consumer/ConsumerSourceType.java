package com.alibaba.tesla.tkgone.server.consumer;

/**
 * @author yangjinghua
 */

public enum ConsumerSourceType {

    // 通过tt获取tddl表的新增信息
    ttReader,
    // 数据库表全量导入
    mysqlTable,
    // ODPS数据导入
    odpsTable,
    // 脚本全量导入
    script,
    // 语雀文档导入
    larkDocument,
    // gitBook的markdown文档导入
    gitBookMarkdownDocument
}
