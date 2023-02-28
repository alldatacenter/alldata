## writeMode

hdfswriter写入前数据清理处理模式：

- **append**: 写入前不做任何处理，DataX hdfswriter直接使用filename写入，并保证文件名不冲突，
- **nonConflict**：如果目录下有fileName前缀的文件，直接报错
