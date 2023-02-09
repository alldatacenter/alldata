/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Interface log class, which is used to format logs.
 * 
 */
public class InterfaceLogBean {
    private static final String DATE_FMT_YYYYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";

    /**
     * Unique ID of the transaction to which an interface message belongs. If no
     * transaction ID exists, this parameter is left empty.
     */
    private String transactionId;

    /**
     * Enter the interface product. For example, enter UC for the UC interface.
     * The products include UC, IVS, TP, FusionSphere, and Storage.
     */
    private String product;

    /**
     * Interface type. The value can be 1 or 2. 1: northbound interface. 2:
     * southbound interface
     */
    private String interfaceType;

    /**
     * Protocol type. The options are SOAP (ParlayX), Rest, COM, Native,
     * HTTP+XML, and SMPP.
     */
    private String protocolType;

    /**
     * Interface name
     */
    private String name;

    /**
     * Source device. The client API class is empty. The parameter is not
     * displayed externally.
     */
    private String sourceAddr;

    /**
     * Sink device. The API class of the client is empty. The parameter is not
     * displayed externally.
     */
    private String targetAddr;

    /**
     * Time when the northbound interface receives a request and time when the
     * southbound interface sends a request.
     */
    private Date reqTime;

    /**
     * Time format: yyyy-MM-dd HH:mm:ss
     */
    private String reqTimeAsString;

    /**
     * Response time of the northbound interface, and time when the southbound
     * interface receives the response.
     */
    private Date respTime;

    /**
     * Time format: yyyy-MM-dd HH:mm:ss
     */
    private String respTimeAsString;

    /**
     * Request parameter. The keyword needs to be replaced with *.
     */
    private String reqParams;

    /**
     * Result code returned by the interface
     */
    private String resultCode;

    /**
     * Response parameter. The keyword needs to be replaced with *.
     */
    private String respParams;

    /**
     * Default InterfaceType 1, Product Storage, ProtocolType HTTP+XML, ReqTime
     * Construction time, sourceAddr Local IP address, transactionId Request ID,
     * which can be the currently generated UUID. Response information needs to
     * be set when the response is returned.
     * 
     * @param name
     *            Interface name
     * @param targetAddr
     *            IP address of the target host
     * @param requestParams
     *            Request parameters
     */
    public InterfaceLogBean(String name, String targetAddr, String reqParams) {
        this.transactionId = "";
        this.interfaceType = "1";
        this.product = "Storage";
        this.protocolType = "HTTP+XML";
        this.reqTime = new Date();
        this.name = name;
        this.sourceAddr = "";
        this.targetAddr = "";
        this.reqParams = reqParams;
    }

    /**
     * Configure the response information.<br/>
     * The default response time is the time when the method is called. You can
     * also call the setRespTime method to specify the response time.
     * 
     * @param respParams
     *            Response parameters
     * @param resultCode
     *            Result code
     * @return
     */
    public void setResponseInfo(String respParams, String resultCode) {
        this.respParams = respParams;
        this.resultCode = resultCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(String sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public String getTargetAddr() {
        return targetAddr;
    }

    public void setTargetAddr(String targetAddr) {
        this.targetAddr = targetAddr;
    }

    public Date getReqTime() {
        if (null == this.reqTime) {
            return null;
        } else {
            return (Date) this.reqTime.clone();
        }
    }

    public void setReqTime(Date reqTime) {
        if (null != reqTime) {
            this.reqTime = (Date) reqTime.clone();
        }
    }

    public String getReqTimeAsString() {
        // DATE_FMT_YYYYMMDDHHMMSS
        if (null == reqTimeAsString && null != reqTime) {
            DateFormat df = new SimpleDateFormat(DATE_FMT_YYYYMMDDHHMMSS);
            return df.format(reqTime);
        }
        return reqTimeAsString;
    }

    public void setReqTimeAsString(String reqTimeAsString) {
        this.reqTimeAsString = reqTimeAsString;
    }

    public Date getRespTime() {
        if (null == this.respTime) {
            return null;
        } else {
            return (Date) this.respTime.clone();
        }
    }

    public void setRespTime(Date respTime) {
        if (null != respTime) {
            this.respTime = (Date) respTime.clone();
        }
    }

    public String getRespTimeAsString() {
        if (null == respTimeAsString && null != respTime) {
            DateFormat df = new SimpleDateFormat(DATE_FMT_YYYYMMDDHHMMSS);
            return df.format(respTime);
        }
        return respTimeAsString;
    }

    public void setRespTimeAsString(String respTimeAsString) {
        this.respTimeAsString = respTimeAsString;
    }

    public String getReqParams() {
        return reqParams;
    }

    public void setReqParams(String reqParams) {
        this.reqParams = reqParams;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getRespParams() {
        return respParams;
    }

    public void setRespParams(String respParams) {
        this.respParams = respParams;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getProduct()).append("|");
        sb.append(this.getInterfaceType()).append("|");
        sb.append(this.getProtocolType()).append("|");
        sb.append(this.getName()).append("|");
        sb.append(this.getSourceAddr()).append("|");
        sb.append(this.getTargetAddr()).append("|");
        sb.append(this.getTransactionId() == null ? "" : this.getTransactionId()).append("|");
        sb.append(this.getReqTimeAsString()).append("|");
        sb.append(this.getRespTimeAsString()).append("|");
        sb.append(this.getReqParams() == null ? "" : this.getReqParams()).append("|");
        sb.append(this.getRespParams() == null ? "" : this.getRespParams()).append("|");
        sb.append(this.getResultCode()).append("|");
        return sb.toString();
    }
}
