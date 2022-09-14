delete from  `dss_workspace_dictionary` WHERE `workspace_id` = '0' and `dic_key` = 'pdp_streamis_product_center';

INSERT INTO `dss_workspace_dictionary` ( `workspace_id`, `parent_key`, `dic_name`, `dic_name_en`, `dic_key`, `dic_value`, `dic_value_en`, `title`, `title_en`, `url`, `url_type`,`icon`, `order_num`, `remark`, `create_user`, `create_time`, `update_user`, `update_time`)
VALUES ('0','p_develop_process','流式生产中心','Streamis Product Center','pdp_streamis_product_center','streamis_prod',NULL,NULL,NULL,
'http://APPCONN_INSTALL_IP:APPCONN_INSTALL_PORT/#/realtimeJobCenter?projectName=${projectName}&workspaceName=${workspaceName}','0','kaifa-icon','1','工程开发流程-流式生产中心','SYSTEM','2020-12-28 17:32:35',NULL,'2022-06-30 17:49:02');

select @old_dss_appconn_id:=id from `dss_appconn` where `appconn_name` = 'streamis';

delete from  `dss_workspace_menu_appconn` WHERE `appconn_id` = @old_dss_appconn_id;
delete from `dss_appconn_instance` where `appconn_id` = @old_dss_appconn_id;
delete from  `dss_appconn` where `appconn_name`='streamis';

select @old_jobcenter_dss_appconn_id:=id from `dss_appconn` where `appconn_name` = 'realTimeJobCenter';

delete from  `dss_workspace_menu_appconn` WHERE `appconn_id` = @old_jobcenter_dss_appconn_id;
delete from `dss_appconn_instance` where `appconn_id` = @old_jobcenter_dss_appconn_id;
delete from  `dss_appconn` where `appconn_name`='realTimeJobCenter';

INSERT INTO dss_appconn
(appconn_name, is_user_need_init, `level`, if_iframe, is_external, reference, class_name, appconn_class_path, resource)
VALUES('streamis', 0, 1, 1, 1, NULL, 'com.webank.wedatasphere.streamis.dss.appconn.StreamisAppConn', NULL, NULL);
INSERT INTO dss_appconn
(appconn_name, is_user_need_init, `level`, if_iframe, is_external, reference, class_name, appconn_class_path, resource)
VALUES('realTimeJobCenter', 0, 1, 1, 1, 'sso', '', NULL, NULL);

select @dss_appconn_id:=id from `dss_appconn` where `appconn_name` = 'streamis';
select @jobcenter_dss_appconn_id:=id from `dss_appconn` where `appconn_name` = 'realTimeJobCenter';

INSERT INTO dss_workspace_menu_appconn
(appconn_id, menu_id, title_en, title_cn, desc_en, desc_cn, labels_en, labels_cn, is_active, access_button_en, access_button_cn, manual_button_en, manual_button_cn, manual_button_url, icon, `order`, create_by, create_time, last_update_time, last_update_user, image)
VALUES(@jobcenter_dss_appconn_id, 1, 'StreamSQL development', 'StreamSQL开发', 'Real-time application development is a streaming solution jointly built by WeDataSphere, Boss big data team and China Telecom ctcloud Big data team.', '实时应用开发是微众银行微数域(WeDataSphere)、Boss直聘大数据团队 和 中国电信天翼云大数据团队 社区联合共建的流式解决方案，以 Linkis 做为内核，基于 Flink Engine 构建的批流统一的 Flink SQL，助力实时化转型。',
'streaming, realtime', '流式,实时', 0, 'under union construction', '联合共建中', 'related information', '相关资讯', 'http://127.0.0.1:8088/wiki/scriptis/manual/workspace_cn.html', 'shujukaifa-logo', NULL, NULL, NULL, NULL, NULL, 'shujukaifa-icon');

INSERT INTO dss_appconn_instance
(appconn_id, label, url, enhance_json, homepage_uri)
VALUES(@dss_appconn_id, 'DEV', 'http://APPCONN_INSTALL_IP:APPCONN_INSTALL_PORT/', '', 'http://APPCONN_INSTALL_IP:APPCONN_INSTALL_PORT/#/realTimeJobCenter');

INSERT INTO dss_appconn_instance
(appconn_id, label, url, enhance_json, homepage_uri)
VALUES(@jobcenter_dss_appconn_id, 'DEV', 'http://APPCONN_INSTALL_IP:APPCONN_INSTALL_PORT/#/realTimeJobCenter', NULL, NULL);