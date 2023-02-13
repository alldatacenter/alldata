ALTER TABLE `dice_release`
    CHANGE `application_release_list` `modes` text NOT NULL COMMENT '依赖的应用制品ID列表'