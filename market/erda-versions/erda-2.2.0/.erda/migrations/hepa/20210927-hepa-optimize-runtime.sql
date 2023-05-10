ALTER TABLE tb_gateway_api ADD INDEX `idx_path` (api_path(128));
ALTER TABLE tb_gateway_api ADD INDEX `idx_runtime` (runtime_service_id);
ALTER TABLE tb_gateway_api ADD INDEX `idx_type_runtime` (redirect_type,runtime_service_id,is_deleted);
ALTER TABLE tb_gateway_route ADD INDEX `idx_api` (api_id,is_deleted);
ALTER TABLE tb_gateway_service ADD INDEX `idx_api` (api_id,is_deleted);
ALTER TABLE tb_gateway_package ADD INDEX `idx_runtime` (runtime_service_id,is_deleted);
ALTER TABLE tb_gateway_package_api ADD INDEX `idx_runtime` (runtime_service_id,is_deleted);
