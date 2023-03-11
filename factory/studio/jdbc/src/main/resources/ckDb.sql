create table order(id UInt32,sku_id String,total_amount UInt32,create_time Datetime) engine =MergeTree partition by toYYYYMMDD(create_time) primary key (id)
  order by (id,sku_id);