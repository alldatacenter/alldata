alter table am_k8s_micro_service_meta
    drop key uk_app_micro_service;

alter table am_k8s_micro_service_meta
    add constraint uk_app_micro_service
        unique (app_id, micro_service_id, namespace_id, stage_id, arch);

