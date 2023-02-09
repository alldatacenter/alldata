/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.region;

import java.io.Serializable;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.internal.UrlComponentUtils;

public class Region implements Serializable {
    private static final long serialVersionUID = 1L;
    private String regionName;
    private String displayName;

    public Region(String regionName) {
        this(regionName, regionName);
    }

    public Region(String regionName, String displayName) {
        this.regionName = regionName;
        this.displayName = displayName;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof Region) {
            String anotherRegionName = ((Region) anObject).getRegionName();
            return anotherRegionName.equals(regionName);
        }
        return false;
    }
    
    public static String formatRegion(Region region) throws CosClientException {
        return formatRegion(region.getRegionName());
    }
    
    public static String formatRegion(String regionName) throws CosClientException {
        UrlComponentUtils.validateRegionName(regionName);
        if (regionName.startsWith("cos.")) {
            return regionName;
        } else {
            if (regionName.equals("cn-east") || regionName.equals("cn-south")
                    || regionName.equals("cn-north") || regionName.equals("cn-south-2")
                    || regionName.equals("cn-southwest") || regionName.equals("sg")) {
                return regionName;
            } else {
                return "cos." + regionName;
            }
        }
    }

    public static String formatCIRegion(Region region) throws CosClientException {
        return formatCIRegion(region.getRegionName());
    }

    public static String formatCIRegion(String regionName) throws CosClientException {
        UrlComponentUtils.validateRegionName(regionName);
        if (regionName.startsWith("ap-")||regionName.startsWith("eu-")||regionName.startsWith("na-")) {
            return regionName;
        } else {
            return "ap-" + regionName;
        }
    }
}
