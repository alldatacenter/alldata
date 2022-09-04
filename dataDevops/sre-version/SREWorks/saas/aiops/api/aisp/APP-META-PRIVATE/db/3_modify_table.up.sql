alter table aisp_scene_config modify `scene_model_param` json NULL COMMENT '场景级别模型参数';
alter table aisp_scene_config add `detector_binder` varchar(128) NOT NULL DEFAULT '*' COMMENT '绑定检测器';