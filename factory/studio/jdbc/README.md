# ClickHouse

```markdown
1、CK创建表
clickhouse-client -h CKHost -d ckDB -m -u test --password test

create table Order(id UInt32,sku_id String,total_amount UInt32,create_time Datetime) 

engine =MergeTree partition by toYYYYMMDD(create_time) primary key (id)

order by (id,sku_id);

2、CK MAPPER写在 com.platform.ck.mapper.clickhouse

3、Order为样例实体类, OrderMapper为CK crud Mapper

4、使用数据源时，直接在com.platform.ck.mapper.clickhouse编写自己的Mapper接口即可

5、SprigBoot启动项目使用时，直接启动项目CRUD访问URL

    增：
    http://localhost:8080/order/addOrder
    {
    "id": 1,
    "skuId": 1053,
    "totalAmount": 111,
    "createTime": "2022-04-25"
    }
    
    删：
    http://localhost:8080/order/orderDel
    {
    "id": 1,
    "skuId": 1002,
    "totalAmount": 1200,
    "createTime": "2022-05-05"
    }
    
    改：
    http://localhost:8080/order/orderEdit
    {
    "id": 4,
    "skuId": 100112,
    "totalAmount": 100,
    "createTime": "2022-05-05"
    }


    查: http://localhost:8080/order/orderList

```
