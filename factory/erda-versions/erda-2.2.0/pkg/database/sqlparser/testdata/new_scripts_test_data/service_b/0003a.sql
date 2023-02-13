ALTER TABLE dice_api_doc_lock
    ADD CONSTRAINT uk_new_constraint UNIQUE KEY (doc_name);

ALTER TABLE dice_api_doc_lock
    MODIFY new_col1 varchar(1024) not null default 'default text' comment 'modified the column';

ALTER TABLE dice_api_doc_lock
    CHANGE new_col1 new_col1 text not null comment 'change the column';

-- ALTER TABLE doc_lock
--     ALTER new_col1 set default 99;

ALTER TABLE dice_api_doc_lock
    RENAME INDEX uk_doc TO uk_doc2;