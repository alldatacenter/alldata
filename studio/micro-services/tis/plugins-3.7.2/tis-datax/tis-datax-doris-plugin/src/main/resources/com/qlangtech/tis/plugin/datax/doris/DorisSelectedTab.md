## seqKey

用户需要确保每次更新记录该列的值会递增，支持使用：整型数字、DATE、DATETIME类型的列，通过设置Sequence可以保证在乱序情况下可以保证数据不会发生脏写

详细请查阅 Doris文档：https://doris.apache.org/zh-CN/docs/dev/data-operate/update-delete/sequence-column-manual
