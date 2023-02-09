package com.qcloud.cos.model.ciModel.image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取图片标签接口响应实体 https://cloud.tencent.com/document/product/460/39082
 */
public class ImageLabelResponse {
    /**
     * 对象在cos中的相对位置，例如 demo/picture.jpg
     */
    private List<Lobel> recognitionResult;

    public List<Lobel> getRecognitionResult() {
        if (recognitionResult == null) {
            recognitionResult = new ArrayList<>();
        }
        return recognitionResult;
    }

    public void setRecognitionResult(List<Lobel> recognitionResult) {
        this.recognitionResult = recognitionResult;
    }

    public String getResultJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(recognitionResult);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageLabelResponse{");
        sb.append("recognitionResult=").append(recognitionResult);
        sb.append('}');
        return sb.toString();
    }
}
