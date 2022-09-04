package com.alibaba.tesla.tkgone.server.domain.vo;

import com.alibaba.tesla.tkgone.server.domain.config.Word;
import io.swagger.annotations.ApiModel;

import java.util.ArrayList;

/**
 * @author feiquan
 */
@ApiModel(value = "保存成功返回列表")
public class SaveWordsResponse extends ArrayList<Word> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public SaveWordsResponse(int initialCapacity) {
        super(initialCapacity);
    }
}
