INSERT INTO `aisp_scene_config` (
    `scene_code`,`gmt_create`,`gmt_modified`,`owners`,`product_name`,`scene_name`,`comment`
) VALUES (
    'default','2022-02-16 15:06:53','2022-02-16 15:06:53','*','swadmin','默认','初始化'
);
INSERT INTO `aisp_detector_config` (
    `detector_code`,`gmt_create`,`gmt_modified`,`detector_url`,`comment`
) VALUES (
    'drilldown','2022-02-18 08:22:10','2022-02-18 08:22:10','prod-aiops-drilldown.sreworks-aiops','drilldown'
);
INSERT INTO `aisp_detector_config` (
    `detector_code`,`gmt_create`,`gmt_modified`,`detector_url`,`comment`
) VALUES (
    'processstrategy','2022-02-18 08:22:10','2022-02-18 08:22:10','prod-aiops-processstrategy.sreworks-aiops','drilldown'
);
INSERT INTO `aisp_detector_config` (
    `detector_code`,`gmt_create`,`gmt_modified`,`detector_url`,`comment`
) VALUES (
    'anomalydetection','2022-02-18 08:22:10','2022-02-18 08:22:10','prod-aiops-anomalydetection.sreworks-aiops','drilldown'
);