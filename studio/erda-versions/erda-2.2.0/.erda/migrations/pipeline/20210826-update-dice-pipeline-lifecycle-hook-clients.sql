-- because the fdp in erda on erda is no longer the default namespace, but the components are in the same ns
-- direct update does not determine whether it exists, because pipeline-base.sql is data created with a fixed id
UPDATE `dice_pipeline_lifecycle_hook_clients` SET host='fdp-master:8080' where id in (1);
