ALTER TABLE dice_api_doc_lock
    COMMENT='修改了的 table option';

ALTER TABLE dice_api_doc_lock
    ADD (
        new_col1 varchar(16) not null comment 'this is a new column',
        new_col2 bigint not null comment ''
    );