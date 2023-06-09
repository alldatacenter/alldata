/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.async.message.client.vo;

import java.io.Serializable;
import java.util.Date;

/*
 * Notify消息 VO
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016-04-22
 */
public class MessageVo implements Serializable {

    private static final long serialVersionUID = -746914705850067717L;

    /**
     * ID 生成一个唯一标识
     */
    private String id;

    /**
     * 餐饮实体ID
     */
    private String entityId;

    /**
     * 数据来源ID,如:用户ID
     */
    private String sourceId;

    /**
     * 桌位号
     */
    private String seatCode;

    /**
     * 消息发送时间
     */
    private Date createTime;

    /**
     * 业务类型主键ID,如 waiting orderId ,orderId ,waitingPayId等
     */
    private String businessId;

    /**
     * 其他需要传递的信息
     */
    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "MessageVo{" + "id='" + id + '\'' + ", entityId='" + entityId + '\'' + ", sourceId='" + sourceId + '\'' + ", seatCode='" + seatCode + '\'' + ", createTime=" + createTime + ", businessId='" + businessId + '\'' + ", content='" + content + '\'' + '}';
    }
}
