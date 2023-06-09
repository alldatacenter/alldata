///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// * <p>
// * This program is free software: you can use, redistribute, and/or modify
// * it under the terms of the GNU Affero General Public License, version 3
// * or later ("AGPL"), as published by the Free Software Foundation.
// * <p>
// * This program is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.
// * <p>
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package com.qlangtech.tis.plugin.datax;
//
//import com.alibaba.datax.plugin.writer.elasticsearchwriter.ESFieldType;
//import com.google.common.collect.Lists;
//import com.qlangtech.tis.manage.common.Option;
//import com.qlangtech.tis.runtime.module.misc.TokenizerType;
//
//import java.util.List;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2021-01-27 13:28
// */
//public enum TISESFieldType {
//    STRING("string"), INT("int"), FLOAT("float"), LONG("long") //
//    , DOUBLE("double"), IK("ik"), TEXT_WS("text_ws"), LIKE("like") //
//    , PINYIN("pinyin")//
//    , DATE("date"), TIMESTAMP("timestamp");
//
//    public void allEsTypes() {
//
//        for (ESFieldType t : com.alibaba.datax.plugin.writer.elasticsearchwriter.ESFieldType.values()) {
//
//        }
//    }
//
//    public static List<Option> all() {
//        List<Option> all = Lists.newArrayList();
//        TokenizerType tokenizerType = null;
//        for (com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType ft : com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType.values()) {
//            tokenizerType = TokenizerType.parse(ft.literia);
//            all.add(new Option(tokenizerType != null ? tokenizerType.getDesc() : ft.literia, ft.literia));
//        }
//        return all;
//    }
//
//    public final String literia;
//
//    private TISESFieldType(String literia) {
//        this.literia = literia;
//    }
//}
