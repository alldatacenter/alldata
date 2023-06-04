package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知组-用户中间表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:28:12
 */
@TableName("t_ddh_notice_group_user")
public class NoticeGroupUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 通知组id
     */
    private Integer noticeGroupId;
    /**
     * 用户id
     */
    private Integer userId;

}
