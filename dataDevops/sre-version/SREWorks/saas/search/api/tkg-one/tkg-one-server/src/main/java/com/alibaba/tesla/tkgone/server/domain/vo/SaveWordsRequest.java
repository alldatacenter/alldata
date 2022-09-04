package com.alibaba.tesla.tkgone.server.domain.vo;

import com.alibaba.tesla.tkgone.server.common.Assert;
import com.alibaba.tesla.tkgone.server.common.entity.Verifiable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author feiquan
 */
@Data
@ApiModel(value = "保存自定义分词请求")
public class SaveWordsRequest implements Verifiable {
    @ApiModelProperty(value = "分词列表", example = "[\"中国\", \"祖国\"]")
    private String[] words;
    @ApiModelProperty(value = "保存备份", position = 1, example = "根据Blink新增概念补充")
    private String memo;

    @Override
    public void verify() throws IllegalArgumentException {
        Assert.notEmpty("words", words);

        for (String word : words) {
            if (StringUtils.isEmpty(word)) {
                throw new IllegalArgumentException("words's element cannot be empty");
            }
        }
    }
}
