-- MIGRATION_BASE

-- maintainer: QvodSoldier
-- added time: 2021/06/04
-- filename: 4.0/migrations/orchestrator/v001/migration.sql

ALTER TABLE ps_runtime_services modify column ports text COMMENT '端口';
