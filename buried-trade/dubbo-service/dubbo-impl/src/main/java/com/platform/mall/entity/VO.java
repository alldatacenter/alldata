package com.platform.mall.entity;

import com.platform.mall.entity.mobile.LitemallGoodsSpecification;

import java.io.Serializable;
import java.util.List;

public class VO implements Serializable {
        private String name;
        private List<LitemallGoodsSpecification> valueList;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<LitemallGoodsSpecification> getValueList() {
            return valueList;
        }

        public void setValueList(List<LitemallGoodsSpecification> valueList) {
            this.valueList = valueList;
        }
    }
