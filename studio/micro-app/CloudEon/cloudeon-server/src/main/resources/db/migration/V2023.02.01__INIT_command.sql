create table ce_command
(
    id                            int auto_increment
        primary key,
    cluster_id                    int          null,
    command_state                 varchar(255) null,
    end_time                      datetime(6)  null,
    name                          varchar(255) null,
    operate_user_id               int          null,
    start_time                    datetime(6)  null,
    submit_time                   datetime(6)  null,
    current_progress                int          null,
    type                          varchar(255) null
);

create table ce_command_task_group
(
    id                    int auto_increment
        primary key,
    command_id            int          null,
    service_instance_id   int          null,
    service_instance_name varchar(255) null,
    stack_service_id      int          null,
    stack_service_name    varchar(255) null,
    step_name             varchar(255) null,
    step_sort_num         int          null
);

create table ce_command_task
(
    id                    int auto_increment
        primary key,
    command_state         varchar(255) null,
    command_task_group_id int          null,
    command_id int          null,
    end_time              datetime(6)  null,
    progress              int          null,
    service_instance_id              int          null,
    service_instance_name              varchar(255)          null,
    processor_class_name varchar(255) null,
    task_log_path varchar(255) null,
    start_time            datetime(6)  null,
    task_name             varchar(255) null,
    task_param             text null,
    task_show_sort_num    int          null
);

