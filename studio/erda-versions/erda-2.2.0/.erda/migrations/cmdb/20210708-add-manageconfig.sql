-- add manage config field to store cluster credential info
ALTER TABLE co_clusters ADD COLUMN `manage_config` text NOT NULL COMMENT "cluster crendtial config";