package cn.datax.service.email.api.entity;

import cn.datax.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "tbl_email", autoResultMap = true)
public class EmailEntity extends BaseEntity {

    /**
     * 标题
     */
    private String subject;
    /**
     * 内容
     */
    private String text;
    /**
     * 接收人
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tos;
    /**
     * 抄送人
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> ccs;
    /**
     * 密送人
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> bccs;
    /**
     * 附件
     */
    @TableField(exist = false)
    private List<File> files;
}
