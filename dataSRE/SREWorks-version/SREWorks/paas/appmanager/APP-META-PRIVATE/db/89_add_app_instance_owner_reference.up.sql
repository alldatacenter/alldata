alter table am_rt_app_instance
    add owner_reference text null comment 'Owner Reference';

alter table am_rt_app_instance
    add parent_owner_reference text null comment 'Parent Owner Reference';

