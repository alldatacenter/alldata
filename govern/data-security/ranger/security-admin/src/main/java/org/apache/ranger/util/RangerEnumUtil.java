/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.util;

/**
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.ranger.common.view.VEnum;
import org.apache.ranger.common.view.VEnumElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RangerEnumUtil {

    private static final Logger logger = LoggerFactory.getLogger(RangerEnumUtil.class);
    public final static String ENUM_CommonEnums_ActiveStatus = "CommonEnums.ActiveStatus";
    public final static String ENUM_CommonEnums_ActivationStatus = "CommonEnums.ActivationStatus";
    public final static String ENUM_CommonEnums_BooleanValue = "CommonEnums.BooleanValue";
    public final static String ENUM_CommonEnums_DataType = "CommonEnums.DataType";
    public final static String ENUM_CommonEnums_DeviceType = "CommonEnums.DeviceType";
    public final static String ENUM_CommonEnums_DiffLevel = "CommonEnums.DiffLevel";
    public final static String ENUM_CommonEnums_FileType = "CommonEnums.FileType";
    public final static String ENUM_CommonEnums_FreqType = "CommonEnums.FreqType";
    public final static String ENUM_CommonEnums_MimeType = "CommonEnums.MimeType";
    public final static String ENUM_CommonEnums_NumberFormat = "CommonEnums.NumberFormat";
    public final static String ENUM_CommonEnums_ObjectStatus = "CommonEnums.ObjectStatus";
    public final static String ENUM_CommonEnums_PasswordResetStatus = "CommonEnums.PasswordResetStatus";
    public final static String ENUM_CommonEnums_PriorityType = "CommonEnums.PriorityType";
    public final static String ENUM_CommonEnums_ProgressStatus = "CommonEnums.ProgressStatus";
    public final static String ENUM_CommonEnums_RelationType = "CommonEnums.RelationType";
    public final static String ENUM_CommonEnums_UserSource = "CommonEnums.UserSource";
    public final static String ENUM_CommonEnums_AssetType = "CommonEnums.AssetType";
    public final static String ENUM_CommonEnums_AccessResult = "CommonEnums.AccessResult";
    public final static String ENUM_CommonEnums_PolicyType = "CommonEnums.PolicyType";
    public final static String ENUM_CommonEnums_XAAuditType = "CommonEnums.XAAuditType";
    public final static String ENUM_CommonEnums_ResourceType = "CommonEnums.ResourceType";
    public final static String ENUM_CommonEnums_XAGroupType = "CommonEnums.XAGroupType";
    public final static String ENUM_CommonEnums_XAPermForType = "CommonEnums.XAPermForType";
    public final static String ENUM_CommonEnums_XAPermType = "CommonEnums.XAPermType";
    public final static String ENUM_CommonEnums_ClassTypes = "CommonEnums.ClassTypes";
    public final static String ENUM_XXAuthSession_AuthStatus = "XXAuthSession.AuthStatus";
    public final static String ENUM_XXAuthSession_AuthType = "XXAuthSession.AuthType";
    public final static String ENUM_XResponse_ResponseStatus = "XResponse.ResponseStatus";

    protected Map<String, VEnum> enumMap = new HashMap<String, VEnum>();
    protected List<VEnum> enumList = new ArrayList<VEnum>();

    public List<VEnum> getEnums() {
	if (enumList.isEmpty()) {
	    init();
	}
	return enumList;
    }

    public VEnum getEnum(String enumName) {
	if (enumList.isEmpty()) {
	    init();
	}
	return enumMap.get(enumName);
    }

    public String getLabel(String enumName, int enumValue) {
	VEnum vEnum = getEnum(enumName);
	if (vEnum == null) {
	    logger.error("Enum " + enumName + " not found.", new Throwable());
	    return "";
	}
	for (VEnumElement vEnumElement : vEnum.getElementList()) {
	    if (vEnumElement.getElementValue() == enumValue) {
		return vEnumElement.getElementLabel();
	    }
	}
	logger.error("Enum value not found. enum=" + enumName + ", value="
		+ enumValue, new Throwable());
	return "";
    }

	public int getValue(String enumName, String elementName) {
		VEnum vEnum = getEnum(enumName);
		if (vEnum == null) {
			logger.error("Enum " + enumName + " not found.", new Throwable());
			return -1;
		}
		for (VEnumElement vEnumElement : vEnum.getElementList()) {
			if (vEnumElement.getElementName().equalsIgnoreCase(elementName)) {
				return vEnumElement.getElementValue();
			}
		}
		logger.error("Enum value not found. enum=" + enumName
				+ ", elementName=" + elementName, new Throwable());
		return -1;
	}

    protected void init() {
	VEnum vEnum;
	VEnumElement vElement;

	///////////////////////////////////
	// CommonEnums::ActiveStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_ActiveStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_DISABLED");
	vElement.setElementValue(0);
	vElement.setElementLabel("Disabled");
	vElement.setRbKey("xa.enum.ActiveStatus.STATUS_DISABLED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_ENABLED");
	vElement.setElementValue(1);
	vElement.setElementLabel("Enabled");
	vElement.setRbKey("xa.enum.ActiveStatus.STATUS_ENABLED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_DELETED");
	vElement.setElementValue(2);
	vElement.setElementLabel("Deleted");
	vElement.setRbKey("xa.enum.ActiveStatus.STATUS_DELETED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::ActivationStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_ActivationStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_DISABLED");
	vElement.setElementValue(0);
	vElement.setElementLabel("Disabled");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_DISABLED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_ACTIVE");
	vElement.setElementValue(1);
	vElement.setElementLabel("Active");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_ACTIVE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_PENDING_APPROVAL");
	vElement.setElementValue(2);
	vElement.setElementLabel("Pending Approval");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_PENDING_APPROVAL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_PENDING_ACTIVATION");
	vElement.setElementValue(3);
	vElement.setElementLabel("Pending Activation");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_PENDING_ACTIVATION");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_REJECTED");
	vElement.setElementValue(4);
	vElement.setElementLabel("Rejected");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_REJECTED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_DEACTIVATED");
	vElement.setElementValue(5);
	vElement.setElementLabel("Deactivated");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_DEACTIVATED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_PRE_REGISTRATION");
	vElement.setElementValue(6);
	vElement.setElementLabel("Registration Pending");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_PRE_REGISTRATION");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACT_STATUS_NO_LOGIN");
	vElement.setElementValue(7);
	vElement.setElementLabel("No login privilege");
	vElement.setRbKey("xa.enum.ActivationStatus.ACT_STATUS_NO_LOGIN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::BooleanValue
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_BooleanValue);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("BOOL_NONE");
	vElement.setElementValue(0);
	vElement.setElementLabel("None");
	vElement.setRbKey("xa.enum.BooleanValue.BOOL_NONE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("BOOL_TRUE");
	vElement.setElementValue(1);
	vElement.setElementLabel("True");
	vElement.setRbKey("xa.enum.BooleanValue.BOOL_TRUE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("BOOL_FALSE");
	vElement.setElementValue(2);
	vElement.setElementLabel("False");
	vElement.setRbKey("xa.enum.BooleanValue.BOOL_FALSE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::DataType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_DataType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_INTEGER");
	vElement.setElementValue(1);
	vElement.setElementLabel("Integer");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_INTEGER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_DOUBLE");
	vElement.setElementValue(2);
	vElement.setElementLabel("Double");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_DOUBLE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_STRING");
	vElement.setElementValue(3);
	vElement.setElementLabel("String");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_STRING");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_BOOLEAN");
	vElement.setElementValue(4);
	vElement.setElementLabel("Boolean");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_BOOLEAN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_DATE");
	vElement.setElementValue(5);
	vElement.setElementLabel("Date");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_DATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_STRING_ENUM");
	vElement.setElementValue(6);
	vElement.setElementLabel("String enumeration");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_STRING_ENUM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_LONG");
	vElement.setElementValue(7);
	vElement.setElementLabel("Long");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_LONG");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DATA_TYPE_INTEGER_ENUM");
	vElement.setElementValue(8);
	vElement.setElementLabel("Integer enumeration");
	vElement.setRbKey("xa.enum.DataType.DATA_TYPE_INTEGER_ENUM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::DeviceType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_DeviceType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("DEVICE_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.DeviceType.DEVICE_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DEVICE_BROWSER");
	vElement.setElementValue(1);
	vElement.setElementLabel("Browser");
	vElement.setRbKey("xa.enum.DeviceType.DEVICE_BROWSER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DEVICE_IPHONE");
	vElement.setElementValue(2);
	vElement.setElementLabel("iPhone");
	vElement.setRbKey("xa.enum.DeviceType.DEVICE_IPHONE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DEVICE_IPAD");
	vElement.setElementValue(3);
	vElement.setElementLabel("iPad");
	vElement.setRbKey("xa.enum.DeviceType.DEVICE_IPAD");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DEVICE_IPOD");
	vElement.setElementValue(4);
	vElement.setElementLabel("iPod");
	vElement.setRbKey("xa.enum.DeviceType.DEVICE_IPOD");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DEVICE_ANDROID");
	vElement.setElementValue(5);
	vElement.setElementLabel("Android");
	vElement.setRbKey("xa.enum.DeviceType.DEVICE_ANDROID");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::DiffLevel
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_DiffLevel);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("DIFF_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.DiffLevel.DIFF_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DIFF_LOW");
	vElement.setElementValue(1);
	vElement.setElementLabel("Low");
	vElement.setRbKey("xa.enum.DiffLevel.DIFF_LOW");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DIFF_MEDIUM");
	vElement.setElementValue(2);
	vElement.setElementLabel("Medium");
	vElement.setRbKey("xa.enum.DiffLevel.DIFF_MEDIUM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("DIFF_HIGH");
	vElement.setElementValue(3);
	vElement.setElementLabel("High");
	vElement.setRbKey("xa.enum.DiffLevel.DIFF_HIGH");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::FileType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_FileType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("FILE_FILE");
	vElement.setElementValue(0);
	vElement.setElementLabel("File");
	vElement.setRbKey("xa.enum.FileType.FILE_FILE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FILE_DIR");
	vElement.setElementValue(1);
	vElement.setElementLabel("Directory");
	vElement.setRbKey("xa.enum.FileType.FILE_DIR");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::FreqType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_FreqType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_NONE");
	vElement.setElementValue(0);
	vElement.setElementLabel("None");
	vElement.setRbKey("xa.enum.FreqType.FREQ_NONE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_MANUAL");
	vElement.setElementValue(1);
	vElement.setElementLabel("Manual");
	vElement.setRbKey("xa.enum.FreqType.FREQ_MANUAL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_HOURLY");
	vElement.setElementValue(2);
	vElement.setElementLabel("Hourly");
	vElement.setRbKey("xa.enum.FreqType.FREQ_HOURLY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_DAILY");
	vElement.setElementValue(3);
	vElement.setElementLabel("Daily");
	vElement.setRbKey("xa.enum.FreqType.FREQ_DAILY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_WEEKLY");
	vElement.setElementValue(4);
	vElement.setElementLabel("Weekly");
	vElement.setRbKey("xa.enum.FreqType.FREQ_WEEKLY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_BI_WEEKLY");
	vElement.setElementValue(5);
	vElement.setElementLabel("Bi Weekly");
	vElement.setRbKey("xa.enum.FreqType.FREQ_BI_WEEKLY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("FREQ_MONTHLY");
	vElement.setElementValue(6);
	vElement.setElementLabel("Monthly");
	vElement.setRbKey("xa.enum.FreqType.FREQ_MONTHLY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::MimeType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_MimeType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("MIME_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.MimeType.MIME_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("MIME_TEXT");
	vElement.setElementValue(1);
	vElement.setElementLabel("Text");
	vElement.setRbKey("xa.enum.MimeType.MIME_TEXT");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("MIME_HTML");
	vElement.setElementValue(2);
	vElement.setElementLabel("Html");
	vElement.setRbKey("xa.enum.MimeType.MIME_HTML");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("MIME_PNG");
	vElement.setElementValue(3);
	vElement.setElementLabel("png");
	vElement.setRbKey("xa.enum.MimeType.MIME_PNG");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("MIME_JPEG");
	vElement.setElementValue(4);
	vElement.setElementLabel("jpeg");
	vElement.setRbKey("xa.enum.MimeType.MIME_JPEG");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::NumberFormat
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_NumberFormat);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("NUM_FORMAT_NONE");
	vElement.setElementValue(0);
	vElement.setElementLabel("None");
	vElement.setRbKey("xa.enum.NumberFormat.NUM_FORMAT_NONE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("NUM_FORMAT_NUMERIC");
	vElement.setElementValue(1);
	vElement.setElementLabel("Numeric");
	vElement.setRbKey("xa.enum.NumberFormat.NUM_FORMAT_NUMERIC");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("NUM_FORMAT_ALPHA");
	vElement.setElementValue(2);
	vElement.setElementLabel("Alphabhet");
	vElement.setRbKey("xa.enum.NumberFormat.NUM_FORMAT_ALPHA");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("NUM_FORMAT_ROMAN");
	vElement.setElementValue(3);
	vElement.setElementLabel("Roman");
	vElement.setRbKey("xa.enum.NumberFormat.NUM_FORMAT_ROMAN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::ObjectStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_ObjectStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("OBJ_STATUS_ACTIVE");
	vElement.setElementValue(0);
	vElement.setElementLabel("Active");
	vElement.setRbKey("xa.enum.ObjectStatus.OBJ_STATUS_ACTIVE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("OBJ_STATUS_DELETED");
	vElement.setElementValue(1);
	vElement.setElementLabel("Deleted");
	vElement.setRbKey("xa.enum.ObjectStatus.OBJ_STATUS_DELETED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("OBJ_STATUS_ARCHIVED");
	vElement.setElementValue(2);
	vElement.setElementLabel("Archived");
	vElement.setRbKey("xa.enum.ObjectStatus.OBJ_STATUS_ARCHIVED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::PasswordResetStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_PasswordResetStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("PWD_RESET_ACTIVE");
	vElement.setElementValue(0);
	vElement.setElementLabel("Active");
	vElement.setRbKey("xa.enum.PasswordResetStatus.PWD_RESET_ACTIVE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PWD_RESET_USED");
	vElement.setElementValue(1);
	vElement.setElementLabel("Used");
	vElement.setRbKey("xa.enum.PasswordResetStatus.PWD_RESET_USED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PWD_RESET_EXPIRED");
	vElement.setElementValue(2);
	vElement.setElementLabel("Expired");
	vElement.setRbKey("xa.enum.PasswordResetStatus.PWD_RESET_EXPIRED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PWD_RESET_DISABLED");
	vElement.setElementValue(3);
	vElement.setElementLabel("Disabled");
	vElement.setRbKey("xa.enum.PasswordResetStatus.PWD_RESET_DISABLED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::PriorityType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_PriorityType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("PRIORITY_NORMAL");
	vElement.setElementValue(0);
	vElement.setElementLabel("Normal");
	vElement.setRbKey("xa.enum.PriorityType.PRIORITY_NORMAL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PRIORITY_LOW");
	vElement.setElementValue(1);
	vElement.setElementLabel("Low");
	vElement.setRbKey("xa.enum.PriorityType.PRIORITY_LOW");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PRIORITY_MEDIUM");
	vElement.setElementValue(2);
	vElement.setElementLabel("Medium");
	vElement.setRbKey("xa.enum.PriorityType.PRIORITY_MEDIUM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PRIORITY_HIGH");
	vElement.setElementValue(3);
	vElement.setElementLabel("High");
	vElement.setRbKey("xa.enum.PriorityType.PRIORITY_HIGH");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::ProgressStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_ProgressStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("PROGRESS_PENDING");
	vElement.setElementValue(0);
	vElement.setElementLabel("Pending");
	vElement.setRbKey("xa.enum.ProgressStatus.PROGRESS_PENDING");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PROGRESS_IN_PROGRESS");
	vElement.setElementValue(1);
	vElement.setElementLabel("In Progress");
	vElement.setRbKey("xa.enum.ProgressStatus.PROGRESS_IN_PROGRESS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PROGRESS_COMPLETE");
	vElement.setElementValue(2);
	vElement.setElementLabel("Complete");
	vElement.setRbKey("xa.enum.ProgressStatus.PROGRESS_COMPLETE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PROGRESS_ABORTED");
	vElement.setElementValue(3);
	vElement.setElementLabel("Aborted");
	vElement.setRbKey("xa.enum.ProgressStatus.PROGRESS_ABORTED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("PROGRESS_FAILED");
	vElement.setElementValue(4);
	vElement.setElementLabel("Failed");
	vElement.setRbKey("xa.enum.ProgressStatus.PROGRESS_FAILED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::RelationType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_RelationType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("REL_NONE");
	vElement.setElementValue(0);
	vElement.setElementLabel("None");
	vElement.setRbKey("xa.enum.RelationType.REL_NONE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("REL_SELF");
	vElement.setElementValue(1);
	vElement.setElementLabel("Self");
	vElement.setRbKey("xa.enum.RelationType.REL_SELF");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::UserSource
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_UserSource);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("USER_APP");
	vElement.setElementValue(0);
	vElement.setElementLabel("Application");
	vElement.setRbKey("xa.enum.UserSource.USER_APP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("USER_GOOGLE");
	vElement.setElementValue(1);
	vElement.setElementLabel("Google");
	vElement.setRbKey("xa.enum.UserSource.USER_GOOGLE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("USER_FB");
	vElement.setElementValue(2);
	vElement.setElementLabel("FaceBook");
	vElement.setRbKey("xa.enum.UserSource.USER_FB");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::AssetType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_AssetType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("ASSET_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.AssetType.ASSET_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ASSET_HDFS");
	vElement.setElementValue(1);
	vElement.setElementLabel("HDFS");
	vElement.setRbKey("xa.enum.AssetType.ASSET_HDFS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ASSET_HBASE");
	vElement.setElementValue(2);
	vElement.setElementLabel("HBase");
	vElement.setRbKey("xa.enum.AssetType.ASSET_HBASE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ASSET_HIVE");
	vElement.setElementValue(3);
	vElement.setElementLabel("Hive");
	vElement.setRbKey("xa.enum.AssetType.ASSET_HIVE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ASSET_AGENT");
	vElement.setElementValue(4);
	vElement.setElementLabel("Agent");
	vElement.setRbKey("xa.enum.AssetType.ASSET_AGENT");
	vElement.setEnumName(vEnum.getEnumName());
	
	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("ASSET_KNOX");
	vElement.setElementValue(5);
	vElement.setElementLabel("Knox");
	vElement.setRbKey("xa.enum.AssetType.ASSET_KNOX");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	
	vElement = new VEnumElement();
	vElement.setElementName("ASSET_STORM");
	vElement.setElementValue(6);
	vElement.setElementLabel("Storm");
	vElement.setRbKey("xa.enum.AssetType.ASSET_STORM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	///////////////////////////////////
	// CommonEnums::AccessResult
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_AccessResult);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("ACCESS_RESULT_DENIED");
	vElement.setElementValue(0);
	vElement.setElementLabel("Denied");
	vElement.setRbKey("xa.enum.AccessResult.ACCESS_RESULT_DENIED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("ACCESS_RESULT_ALLOWED");
	vElement.setElementValue(1);
	vElement.setElementLabel("Allowed");
	vElement.setRbKey("xa.enum.AccessResult.ACCESS_RESULT_ALLOWED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::PolicyType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_PolicyType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("POLICY_INCLUSION");
	vElement.setElementValue(0);
	vElement.setElementLabel("Inclusion");
	vElement.setRbKey("xa.enum.PolicyType.POLICY_INCLUSION");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("POLICY_EXCLUSION");
	vElement.setElementValue(1);
	vElement.setElementLabel("Exclusion");
	vElement.setRbKey("xa.enum.PolicyType.POLICY_EXCLUSION");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::XAAuditType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_XAAuditType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_ALL");
	vElement.setElementValue(1);
	vElement.setElementLabel("All");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_ALL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_READ");
	vElement.setElementValue(2);
	vElement.setElementLabel("Read");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_READ");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_WRITE");
	vElement.setElementValue(3);
	vElement.setElementLabel("Write");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_WRITE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_CREATE");
	vElement.setElementValue(4);
	vElement.setElementLabel("Create");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_CREATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_DELETE");
	vElement.setElementValue(5);
	vElement.setElementLabel("Delete");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_DELETE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_AUDIT_TYPE_LOGIN");
	vElement.setElementValue(6);
	vElement.setElementLabel("Login");
	vElement.setRbKey("xa.enum.XAAuditType.XA_AUDIT_TYPE_LOGIN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::ResourceType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_ResourceType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_PATH");
	vElement.setElementValue(1);
	vElement.setElementLabel("Path");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_PATH");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_DB");
	vElement.setElementValue(2);
	vElement.setElementLabel("Database");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_DB");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_TABLE");
	vElement.setElementValue(3);
	vElement.setElementLabel("Table");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_TABLE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_COL_FAM");
	vElement.setElementValue(4);
	vElement.setElementLabel("Column Family");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_COL_FAM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_COLUMN");
	vElement.setElementValue(5);
	vElement.setElementLabel("Column");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_COLUMN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_VIEW");
	vElement.setElementValue(6);
	vElement.setElementLabel("VIEW");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_VIEW");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_UDF");
	vElement.setElementValue(7);
	vElement.setElementLabel("UDF");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_UDF");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_VIEW_COL");
	vElement.setElementValue(8);
	vElement.setElementLabel("View Column");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_VIEW_COL");
	vElement.setEnumName(vEnum.getEnumName());
	
	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_TOPOLOGY");
	vElement.setElementValue(9);
	vElement.setElementLabel("Topology");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_TOPOLOGY");
	vElement.setEnumName(vEnum.getEnumName());
	
	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_SERVICE");
	vElement.setElementValue(10);
	vElement.setElementLabel("Service");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_SERVICE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("RESOURCE_GLOBAL");
	vElement.setElementValue(11);
	vElement.setElementLabel("Global");
	vElement.setRbKey("xa.enum.ResourceType.RESOURCE_GLOBAL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	///////////////////////////////////
	// CommonEnums::XAGroupType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_XAGroupType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("XA_GROUP_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.XAGroupType.XA_GROUP_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_GROUP_USER");
	vElement.setElementValue(1);
	vElement.setElementLabel("User");
	vElement.setRbKey("xa.enum.XAGroupType.XA_GROUP_USER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_GROUP_GROUP");
	vElement.setElementValue(2);
	vElement.setElementLabel("Group");
	vElement.setRbKey("xa.enum.XAGroupType.XA_GROUP_GROUP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_GROUP_ROLE");
	vElement.setElementValue(3);
	vElement.setElementLabel("Role");
	vElement.setRbKey("xa.enum.XAGroupType.XA_GROUP_ROLE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::XAPermForType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_XAPermForType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_FOR_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.XAPermForType.XA_PERM_FOR_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_FOR_USER");
	vElement.setElementValue(1);
	vElement.setElementLabel("Permission for Users");
	vElement.setRbKey("xa.enum.XAPermForType.XA_PERM_FOR_USER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_FOR_GROUP");
	vElement.setElementValue(2);
	vElement.setElementLabel("Permission for Groups");
	vElement.setRbKey("xa.enum.XAPermForType.XA_PERM_FOR_GROUP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// CommonEnums::XAPermType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_XAPermType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_RESET");
	vElement.setElementValue(1);
	vElement.setElementLabel("Reset");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_RESET");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_READ");
	vElement.setElementValue(2);
	vElement.setElementLabel("Read");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_READ");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_WRITE");
	vElement.setElementValue(3);
	vElement.setElementLabel("Write");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_WRITE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_CREATE");
	vElement.setElementValue(4);
	vElement.setElementLabel("Create");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_CREATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_DELETE");
	vElement.setElementValue(5);
	vElement.setElementLabel("Delete");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_DELETE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_ADMIN");
	vElement.setElementValue(6);
	vElement.setElementLabel("Admin");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_ADMIN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_OBFUSCATE");
	vElement.setElementValue(7);
	vElement.setElementLabel("Obfuscate");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_OBFUSCATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_MASK");
	vElement.setElementValue(8);
	vElement.setElementLabel("Mask");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_MASK");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_EXECUTE");
	vElement.setElementValue(9);
	vElement.setElementLabel("Execute");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_EXECUTE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_SELECT");
	vElement.setElementValue(10);
	vElement.setElementLabel("Select");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_SELECT");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_UPDATE");
	vElement.setElementValue(11);
	vElement.setElementLabel("Update");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_UPDATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_DROP");
	vElement.setElementValue(12);
	vElement.setElementLabel("Drop");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_DROP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_ALTER");
	vElement.setElementValue(13);
	vElement.setElementLabel("Alter");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_ALTER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_INDEX");
	vElement.setElementValue(14);
	vElement.setElementLabel("Index");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_INDEX");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_LOCK");
	vElement.setElementValue(15);
	vElement.setElementLabel("Lock");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_LOCK");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_ALL");
	vElement.setElementValue(16);
	vElement.setElementLabel("All");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_ALL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_ALLOW");
	vElement.setElementValue(17);
	vElement.setElementLabel("Allow");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_ALLOW");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_SUBMIT_TOPOLOGY");
	vElement.setElementValue(18);
	vElement.setElementLabel("Submit Topology");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_SUBMIT_TOPOLOGY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_FILE_UPLOAD");
	vElement.setElementValue(19);
	vElement.setElementLabel("File Upload");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_FILE_UPLOAD");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_GET_NIMBUS");
	vElement.setElementValue(20);
	vElement.setElementLabel("Get Nimbus Conf");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_GET_NIMBUS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_GET_CLUSTER_INFO");
	vElement.setElementValue(21);
	vElement.setElementLabel("Get Cluster Info");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_GET_CLUSTER_INFO");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_FILE_DOWNLOAD");
	vElement.setElementValue(22);
	vElement.setElementLabel("File Download");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_FILE_DOWNLOAD");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_KILL_TOPOLOGY");
	vElement.setElementValue(23);
	vElement.setElementLabel("Kill Topology");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_KILL_TOPOLOGY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_REBALANCE");
	vElement.setElementValue(24);
	vElement.setElementLabel("Rebalance");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_REBALANCE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_ACTIVATE");
	vElement.setElementValue(25);
	vElement.setElementLabel("Activate");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_ACTIVATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_DEACTIVATE");
	vElement.setElementValue(26);
	vElement.setElementLabel("Deactivate");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_DEACTIVATE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_GET_TOPOLOGY_CONF");
	vElement.setElementValue(27);
	vElement.setElementLabel("Get Topology Conf");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_GET_TOPOLOGY_CONF");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_GET_TOPOLOGY");
	vElement.setElementValue(28);
	vElement.setElementLabel("Get Topology");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_GET_TOPOLOGY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_GET_USER_TOPOLOGY");
	vElement.setElementValue(29);
	vElement.setElementLabel("Get User Topology");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_GET_USER_TOPOLOGY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_GET_TOPOLOGY_INFO");
	vElement.setElementValue(30);
	vElement.setElementLabel("Get Topology Info");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_GET_TOPOLOGY_INFO");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);
	
	vElement = new VEnumElement();
	vElement.setElementName("XA_PERM_TYPE_UPLOAD_NEW_CREDENTIAL");
	vElement.setElementValue(31);
	vElement.setElementLabel("Upload New Credential");
	vElement.setRbKey("xa.enum.XAPermType.XA_PERM_TYPE_UPLOAD_NEW_CREDENTIAL");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	
	///////////////////////////////////
	// CommonEnums::ClassTypes
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_CommonEnums_ClassTypes);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_NONE");
	vElement.setElementValue(0);
	vElement.setElementLabel("None");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_NONE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_MESSAGE");
	vElement.setElementValue(1);
	vElement.setElementLabel("Message");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_MESSAGE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_USER_PROFILE");
	vElement.setElementValue(2);
	vElement.setElementLabel("User Profile");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_USER_PROFILE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_AUTH_SESS");
	vElement.setElementValue(3);
	vElement.setElementLabel("Authentication Session");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_AUTH_SESS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_DATA_OBJECT");
	vElement.setElementValue(4);
	vElement.setElementLabel("CLASS_TYPE_DATA_OBJECT");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_DATA_OBJECT");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_NAMEVALUE");
	vElement.setElementValue(5);
	vElement.setElementLabel("CLASS_TYPE_NAMEVALUE");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_NAMEVALUE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_LONG");
	vElement.setElementValue(6);
	vElement.setElementLabel("CLASS_TYPE_LONG");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_LONG");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_PASSWORD_CHANGE");
	vElement.setElementValue(7);
	vElement.setElementLabel("CLASS_TYPE_PASSWORD_CHANGE");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_PASSWORD_CHANGE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_STRING");
	vElement.setElementValue(8);
	vElement.setElementLabel("CLASS_TYPE_STRING");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_STRING");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_ENUM");
	vElement.setElementValue(9);
	vElement.setElementLabel("CLASS_TYPE_ENUM");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_ENUM");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_ENUM_ELEMENT");
	vElement.setElementValue(10);
	vElement.setElementLabel("CLASS_TYPE_ENUM_ELEMENT");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_ENUM_ELEMENT");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_RESPONSE");
	vElement.setElementValue(11);
	vElement.setElementLabel("Response");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_RESPONSE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_ASSET");
	vElement.setElementValue(1000);
	vElement.setElementLabel("Asset");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_ASSET");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_RESOURCE");
	vElement.setElementValue(1001);
	vElement.setElementLabel("Resource");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_RESOURCE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_GROUP");
	vElement.setElementValue(1002);
	vElement.setElementLabel("XA Group");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_GROUP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_USER");
	vElement.setElementValue(1003);
	vElement.setElementLabel("XA User");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_USER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_GROUP_USER");
	vElement.setElementValue(1004);
	vElement.setElementLabel("XA Group of Users");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_GROUP_USER");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_GROUP_GROUP");
	vElement.setElementValue(1005);
	vElement.setElementLabel("XA Group of groups");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_GROUP_GROUP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_PERM_MAP");
	vElement.setElementValue(1006);
	vElement.setElementLabel("XA permissions for resource");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_PERM_MAP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_AUDIT_MAP");
	vElement.setElementValue(1007);
	vElement.setElementLabel("XA audits for resource");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_AUDIT_MAP");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_CRED_STORE");
	vElement.setElementValue(1008);
	vElement.setElementLabel("XA credential store");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_CRED_STORE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_POLICY_EXPORT_AUDIT");
	vElement.setElementValue(1009);
	vElement.setElementLabel("XA Policy Export Audit");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_POLICY_EXPORT_AUDIT");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_TRX_LOG");
	vElement.setElementValue(1010);
	vElement.setElementLabel("Transaction log");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_TRX_LOG");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_ACCESS_AUDIT");
	vElement.setElementValue(1011);
	vElement.setElementLabel("Access Audit");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_ACCESS_AUDIT");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE");
	vElement.setElementValue(1012);
	vElement.setElementLabel("Transaction log attribute");
	vElement.setRbKey("xa.enum.ClassTypes.CLASS_TYPE_XA_TRANSACTION_LOG_ATTRIBUTE");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// XXAuthSession::AuthStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_XXAuthSession_AuthStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_SUCCESS");
	vElement.setElementValue(1);
	vElement.setElementLabel("Success");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_SUCCESS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_WRONG_PASSWORD");
	vElement.setElementValue(2);
	vElement.setElementLabel("Wrong Password");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_WRONG_PASSWORD");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_DISABLED");
	vElement.setElementValue(3);
	vElement.setElementLabel("Account Disabled");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_DISABLED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_LOCKED");
	vElement.setElementValue(4);
	vElement.setElementLabel("Locked");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_LOCKED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_PASSWORD_EXPIRED");
	vElement.setElementValue(5);
	vElement.setElementLabel("Password Expired");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_PASSWORD_EXPIRED");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_STATUS_USER_NOT_FOUND");
	vElement.setElementValue(6);
	vElement.setElementLabel("User not found");
	vElement.setRbKey("xa.enum.AuthStatus.AUTH_STATUS_USER_NOT_FOUND");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


	///////////////////////////////////
	// XXAuthSession::AuthType
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_XXAuthSession_AuthType);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_TYPE_UNKNOWN");
	vElement.setElementValue(0);
	vElement.setElementLabel("Unknown");
	vElement.setRbKey("xa.enum.AuthType.AUTH_TYPE_UNKNOWN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_TYPE_PASSWORD");
	vElement.setElementValue(1);
	vElement.setElementLabel("Username/Password");
	vElement.setRbKey("xa.enum.AuthType.AUTH_TYPE_PASSWORD");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_TYPE_KERBEROS");
	vElement.setElementValue(2);
	vElement.setElementLabel("Kerberos");
	vElement.setRbKey("xa.enum.AuthType.AUTH_TYPE_KERBEROS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_TYPE_SSO");
	vElement.setElementValue(3);
	vElement.setElementLabel("SingleSignOn");
	vElement.setRbKey("xa.enum.AuthType.AUTH_TYPE_SSO");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("AUTH_TYPE_TRUSTED_PROXY");
	vElement.setElementValue(4);
	vElement.setElementLabel("Trusted Proxy");
	vElement.setRbKey("xa.enum.AuthType.AUTH_TYPE_TRUSTED_PROXY");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	///////////////////////////////////
	// XResponse::ResponseStatus
	///////////////////////////////////
	vEnum = new VEnum();
	vEnum.setEnumName(ENUM_XResponse_ResponseStatus);
	vEnum.setElementList(new ArrayList<VEnumElement>());
	enumList.add(vEnum);
	enumMap.put(vEnum.getEnumName(), vEnum);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_SUCCESS");
	vElement.setElementValue(0);
	vElement.setElementLabel("Success");
	vElement.setRbKey("xa.enum.ResponseStatus.STATUS_SUCCESS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_ERROR");
	vElement.setElementValue(1);
	vElement.setElementLabel("Error");
	vElement.setRbKey("xa.enum.ResponseStatus.STATUS_ERROR");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_VALIDATION");
	vElement.setElementValue(2);
	vElement.setElementLabel("Validation Error");
	vElement.setRbKey("xa.enum.ResponseStatus.STATUS_VALIDATION");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_WARN");
	vElement.setElementValue(3);
	vElement.setElementLabel("Warning");
	vElement.setRbKey("xa.enum.ResponseStatus.STATUS_WARN");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_INFO");
	vElement.setElementValue(4);
	vElement.setElementLabel("Information");
	vElement.setRbKey("xa.enum.ResponseStatus.STATUS_INFO");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);

	vElement = new VEnumElement();
	vElement.setElementName("STATUS_PARTIAL_SUCCESS");
	vElement.setElementValue(5);
	vElement.setElementLabel("Partial Success");
	vElement.setRbKey("xa.enum.ResponseStatus.STATUS_PARTIAL_SUCCESS");
	vElement.setEnumName(vEnum.getEnumName());

	vEnum.getElementList().add(vElement);


    }

}

