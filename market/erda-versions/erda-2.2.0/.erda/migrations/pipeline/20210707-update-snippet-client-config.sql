-- because qa and gittar-adaptor are merged into the dop module, some snippet config clients need to change the address to dop
-- direct update does not determine whether it exists, because pipeline-base.sql is data created with a fixed id
UPDATE `dice_pipeline_snippet_clients` SET host='dop:9527' where id in (1, 2);
