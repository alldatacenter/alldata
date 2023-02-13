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
@ApiModel(value = "保存同义词请求")
public class SaveSynonymRequest implements Verifiable {
    @ApiModelProperty(value = "同义词根", example = "中国")
    private String word;
    @ApiModelProperty(value = "同义词列表", example = "[\"China\", \"中华人民共和国\"]")
    private String[] synonyms;
    @ApiModelProperty(value = "保存备份", position = 1, example = "根据Blink新增概念补充")
    private String memo;

    @Override
    public void verify() throws IllegalArgumentException {
        Assert.notEmpty("word", word);
        Assert.notEmpty("synonyms", synonyms);

        for (String synonym : synonyms) {
            if (StringUtils.isEmpty(synonym)) {
                throw new IllegalArgumentException("synonyms's element cannot be empty");
            }
        }
    }
}
