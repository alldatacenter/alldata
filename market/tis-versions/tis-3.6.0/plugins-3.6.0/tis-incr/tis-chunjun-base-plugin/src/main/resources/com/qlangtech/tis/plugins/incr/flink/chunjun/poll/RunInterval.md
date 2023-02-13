## useMaxFunc

描述：用于标记是否保存endLocation位置的一条或多条数据，true：不保存，false(默认)：保存， 某些情况下可能出现最后几条数据被重复记录的情况，可以将此参数配置为true

useMaxFunc的使用场景
​ 考虑可能存在这样的场景：某一次增量同步后的endLocation为x，在下一次增量同步作业启动的间隙中，表内又写入了增量键的值=x的数据。按照默认的情况，假设增量键为id，下一次作业会拼接例如SELECT id,name,age FROM table WHERE id > x。此时在间隙中插入的id=x的数据将会丢失。

​ 为了对应上述场景，chunjun增量同步提供了配置项useMaxFunc（默认值为false）。在设置useMaxFunc=true时，chunjun会在增量作业启动时获取当前数据库中增量键的最大值作为本次作业的endLocation，并且将用于startLocation的运算符号从'>'改为'>='。例如：

某一次增量启动时上次作业的endLocation为10，id最大值为100，那么将会拼接SQL语句 SELECT id,name,age FROM table WHERE id >= 10 AND id < 100
下一次增量作业启动时id的最大值为200，那么将会拼接SQL语句 SELECT id,name,age FROM table WHERE id >=100 AND id < 200

## startLocation

Chunjun 增量消费启动起始位点支持三种模式：

* `Latest`:
* `Initial`:
* `Designated`:
