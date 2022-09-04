CREATE TABLE `metric_time_series_prediction_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `title` varchar(128) NOT NULL COMMENT '配置名称',
  `metric_id` varchar(128) NOT NULL COMMENT '指标ID',
  `algorithm_id` int NOT NULL,
  `algorithm_param_id` varchar(32) NOT NULL,
  `preprocess_param_id` varchar(32) NOT NULL,
  `min_train_hours` int DEFAULT '3' COMMENT '最小训练时长(时)',
  `default_train_hours` int DEFAULT '168' COMMENT '默认训练时长(时)',
  `series_interval` int DEFAULT '60' COMMENT '时间序列数据间隔(秒)',
  `horizon` bigint DEFAULT '10800' COMMENT '时间序列预测时长(秒)',
  `trigger_interval` int DEFAULT '900' COMMENT '时间序列预测触发间隔(秒)',
  `enable` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `creator` varchar(32) NOT NULL DEFAULT '' COMMENT '创建人',
  `owners` varchar(128) NOT NULL DEFAULT '' COMMENT 'owner列表',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '说明',
  PRIMARY KEY (`id`),
  KEY `key_metric_id` (`metric_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE `time_series_prediction_algorithms` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `time_series_prediction_algorithm_params` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tsp_algorithm_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `time_series_prediction_preprocess_params` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT INTO `time_series_prediction_algorithms` VALUES (1, 'robust_pred'), (2, 'quantile_pred');

INSERT INTO `time_series_prediction_algorithm_params` VALUES (1, 1, 'robust_pred默认参数(默认周期为1天)',  '{"stl_params": {"trend_toggle": true, "noise_truncate": 2, "season_neighbour_wdw_size": 20, "trend_solver_method": "GADMM", "trend_vlambda": 50, "trend_solver_show_progress": false, "season_sigma_i": 0.2, "trend_vlambda_diff": 5, "trend_solver_maxiters": 50, "latest_decomp_length": null, "season_bilateral_period_num": 2, "noise_toggle": true, "noise_sigma_d": 5, "data_T": null, "trend_down_sample": 6, "season_sigma_d": 2, "noise_sigma_i": 0.2, "latest_decomp_output": true}, "stl_detector": "Robust_STL", "interval_sigma": 2, "pd_detector": "ACF_Med", "pd_params": {"period_candi": 1440, "refinePeriod_toggle": true, "refine_tolerance": 0.05, "acf_peak_th": 0.15}, "non_zero_lower_interval": true,"expsmooth_interval_sigma":1}'), (2, 2, 'quantile_pred默认参数', '{"stl_params": {"trend_toggle": true, "noise_truncate": 2, "season_neighbour_wdw_size": 20, "trend_solver_method": "GADMM", "trend_vlambda": 50, "trend_solver_show_progress": false, "season_sigma_i": 0.2, "trend_vlambda_diff": 5, "trend_solver_maxiters": 50, "latest_decomp_length": null, "season_bilateral_period_num": 2, "noise_toggle": true, "noise_sigma_d": 5, "data_T": null, "trend_down_sample": 6, "season_sigma_d": 2, "noise_sigma_i": 0.2, "latest_decomp_output": true}, "stl_detector": "Robust_STL", "interval_sigma": 2, "pd_detector": "ACF_Med", "pd_params": {"period_candi": 1440, "refinePeriod_toggle": true, "refine_tolerance": 0.05, "acf_peak_th": 0.15}, "non_zero_lower_interval": true,"expsmooth_interval_sigma":1,"quantile":0.75}');

INSERT INTO `time_series_prediction_preprocess_params` VALUES (1, '仅做填补缺失值', '{"colname": "kpi", "process_method": {"fillna": {"fillvalue": true}}}');