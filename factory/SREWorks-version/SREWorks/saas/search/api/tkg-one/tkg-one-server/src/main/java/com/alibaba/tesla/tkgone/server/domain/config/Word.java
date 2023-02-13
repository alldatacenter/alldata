package com.alibaba.tesla.tkgone.server.domain.config;

import lombok.Data;

import java.util.Date;

/**
 * 自定义分词
 *
 * @author feiquan
 */
@Data
public class Word {
    private int id;
    private Date gmtCreate;
    private Date gmtModified;
    private String word;
    private String memo;
}
