CREATE TABLE totalpayinfo
(
  "id" VARCHAR(32) ,
  "update_time" DATETIME   NULL,
  "entity_id" VARCHAR(10) NULL ,
  "num" int(11) NULL ,
  "create_time" bigint(20) ,
  "update_date" DATE       NULL,
  "start_time"  DATETIME   NULL
, CONSTRAINT uk_totalpayinfo_unique_id UNIQUE(id)
)
