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

package com.obs.test.internal;

import org.junit.Test;

import com.obs.services.internal.IConvertor;
import com.obs.services.internal.ObsConvertor;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.CanonicalGrantee;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GranteeInterface;
import com.obs.services.model.GroupGrantee;
import com.obs.services.model.Permission;
import com.obs.test.RequestPaymentTest;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ObsConvertorTest {
    private static final Logger logger = LogManager.getLogger(ObsConvertorTest.class);
    
    @Test
    public void test_transBucketLoggingConfiguration_1() {
        IConvertor obsConvertor = ObsConvertor.getInstance();
        
        BucketLoggingConfiguration config = new BucketLoggingConfiguration("test_bucket", "object/prefix");
        config.setAgency("log_config_agency");
        
        CanonicalGrantee grantee = new CanonicalGrantee("domain_id_grantee");
        
        GrantAndPermission targetGrants = new GrantAndPermission(grantee, Permission.PERMISSION_READ);
        config.addTargetGrant(targetGrants);
        String result = obsConvertor.transBucketLoggingConfiguration(config);
        
        logger.info(result);
        String assertStr = "<BucketLoggingStatus><Agency>log_config_agency</Agency><LoggingEnabled><TargetBucket>test_bucket</TargetBucket><TargetPrefix>object/prefix</TargetPrefix><TargetGrants><Grant><Grantee><ID>domain_id_grantee</ID></Grantee><Permission>READ</Permission></Grant></TargetGrants></LoggingEnabled></BucketLoggingStatus>";
        
        assertEquals(assertStr, result);
    }
    
    @Test
    public void test_transBucketLoggingConfiguration_2() {
        IConvertor obsConvertor = ObsConvertor.getInstance();
        
        BucketLoggingConfiguration config = new BucketLoggingConfiguration("test_bucket", "object/prefix");
        config.setAgency("log_config_agency");
        
        GrantAndPermission targetGrants = new GrantAndPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);
        config.addTargetGrant(targetGrants);
        String result = obsConvertor.transBucketLoggingConfiguration(config);
        
        logger.info(result);
        String assertStr = "<BucketLoggingStatus><Agency>log_config_agency</Agency><LoggingEnabled><TargetBucket>test_bucket</TargetBucket><TargetPrefix>object/prefix</TargetPrefix><TargetGrants><Grant><Grantee><Canned>Everyone</Canned></Grantee><Permission>READ</Permission></Grant></TargetGrants></LoggingEnabled></BucketLoggingStatus>";
        
        assertEquals(assertStr, result);
    }
}
