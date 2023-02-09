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

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.obs.services.internal.IConvertor;
import com.obs.services.internal.V2Convertor;
import com.obs.services.model.AbstractNotification.Filter;
import com.obs.services.model.AbstractNotification.Filter.FilterRule;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.EventTypeEnum;
import com.obs.services.model.FunctionGraphConfiguration;
import com.obs.services.model.ProtocolEnum;
import com.obs.services.model.Redirect;
import com.obs.services.model.RedirectAllRequest;
import com.obs.services.model.RouteRule;
import com.obs.services.model.RouteRuleCondition;
import com.obs.services.model.TopicConfiguration;
import com.obs.services.model.WebsiteConfiguration;

public class V2ConvertorTest {
    private static final Logger logger = LogManager.getLogger(V2ConvertorTest.class);

    @Test
    public void test_transBucketNotificationConfiguration() {
        IConvertor v2Convertor = V2Convertor.getInstance();

        BucketNotificationConfiguration config = new BucketNotificationConfiguration();

        TopicConfiguration topicConfig = new TopicConfiguration();
        topicConfig.setId("id_topicConfig");
        topicConfig.setTopic("your topic");
        topicConfig.getEventTypes().add(EventTypeEnum.OBJECT_CREATED_ALL);
        topicConfig.getEventTypes().add(EventTypeEnum.OBJECT_CREATED_COPY);
        Filter topicFilter = new Filter();
        topicFilter.getFilterRules().add(new FilterRule("prefix", "topicConfigSmn"));
        topicFilter.getFilterRules().add(new FilterRule("suffix", ".topicConfigJpg"));
        topicConfig.setFilter(topicFilter);
        config.addTopicConfiguration(topicConfig);

        FunctionGraphConfiguration functionConfig = new FunctionGraphConfiguration();
        functionConfig.setId("id_functionConfig");
        functionConfig.setFunctionGraph("your function");
        functionConfig.getEventTypes().add(EventTypeEnum.OBJECT_CREATED_ALL);
        functionConfig.getEventTypes().add(EventTypeEnum.OBJECT_CREATED_POST);
        Filter functionFilter = new Filter();
        functionFilter.getFilterRules().add(new FilterRule("prefix", "functionConfigFunction"));
        functionFilter.getFilterRules().add(new FilterRule("suffix", ".functionConfigMp4"));
        functionConfig.setFilter(functionFilter);
        config.addFunctionGraphConfiguration(functionConfig);

        String result = v2Convertor.transBucketNotificationConfiguration(config);

        logger.info(result);

        String assertStr = "<NotificationConfiguration><TopicConfiguration><Id>id_topicConfig</Id><Filter><S3Key><FilterRule><Name>prefix</Name><Value>topicConfigSmn</Value></FilterRule><FilterRule><Name>suffix</Name><Value>.topicConfigJpg</Value></FilterRule></S3Key></Filter><Topic>your topic</Topic><Event>s3:ObjectCreated:*</Event><Event>s3:ObjectCreated:Copy</Event></TopicConfiguration><FunctionGraphConfiguration><Id>id_functionConfig</Id><Filter><S3Key><FilterRule><Name>prefix</Name><Value>functionConfigFunction</Value></FilterRule><FilterRule><Name>suffix</Name><Value>.functionConfigMp4</Value></FilterRule></S3Key></Filter><FunctionGraph>your function</FunctionGraph><Event>s3:ObjectCreated:*</Event><Event>s3:ObjectCreated:Post</Event></FunctionGraphConfiguration></NotificationConfiguration>";

        assertEquals(assertStr, result);
    }

    @Test
    public void test_transWebsiteConfiguration_with_redirectAllRequestsTo() {
        IConvertor v2Convertor = V2Convertor.getInstance();

        WebsiteConfiguration config = new WebsiteConfiguration();

        RedirectAllRequest redirectAllRequestsTo = new RedirectAllRequest();
        redirectAllRequestsTo.setHostName("test.host.com");
        redirectAllRequestsTo.setRedirectProtocol(ProtocolEnum.HTTP);
        config.setRedirectAllRequestsTo(redirectAllRequestsTo);
        
        String result = v2Convertor.transWebsiteConfiguration(config);

        logger.info(result);

        String assertStr = "<WebsiteConfiguration><RedirectAllRequestsTo><HostName>test.host.com</HostName><Protocol>http</Protocol></RedirectAllRequestsTo></WebsiteConfiguration>";

        assertEquals(assertStr, result);
    }
    
    @Test
    public void test_transWebsiteConfiguration_without_redirectAllRequestsTo() {
        IConvertor v2Convertor = V2Convertor.getInstance();

        WebsiteConfiguration config = new WebsiteConfiguration();

//        RedirectAllRequest redirectAllRequestsTo = new RedirectAllRequest();
//        redirectAllRequestsTo.setHostName("test.host.com");
//        redirectAllRequestsTo.setRedirectProtocol(ProtocolEnum.HTTP);
//        config.setRedirectAllRequestsTo(redirectAllRequestsTo);
        
        config.setKey("key_test_transWebsiteConfiguration_without_redirectAllRequestsTo");
        
        config.setSuffix("suffix_test_transWebsiteConfiguration_without_redirectAllRequestsTo");
        
        RouteRule routeRule_1 = new RouteRule();
        Redirect r_1 = new Redirect();
        r_1.setHostName("www.routeRule_1.com");
        r_1.setHttpRedirectCode("306");
        r_1.setRedirectProtocol(ProtocolEnum.HTTP);
        r_1.setReplaceKeyPrefixWith("replacekeyprefix_routeRule_1");
        routeRule_1.setRedirect(r_1);
        RouteRuleCondition condition_1 = new RouteRuleCondition();
        condition_1.setHttpErrorCodeReturnedEquals("405");
        condition_1.setKeyPrefixEquals("keyprefix_routeRule_1");
        routeRule_1.setCondition(condition_1);
        config.getRouteRules().add(routeRule_1);
        
        
        RouteRule routeRule_2 = new RouteRule();
        Redirect r_2 = new Redirect();
        r_2.setHostName("www.routeRule_2.com");
        r_2.setHttpRedirectCode("304");
        r_2.setRedirectProtocol(ProtocolEnum.HTTPS);
        r_2.setReplaceKeyPrefixWith("replacekeyprefix_routeRule_2");
        routeRule_2.setRedirect(r_2);
        RouteRuleCondition condition_2 = new RouteRuleCondition();
        condition_2.setHttpErrorCodeReturnedEquals("404");
        condition_2.setKeyPrefixEquals("keyprefix_routeRule_2");
        routeRule_2.setCondition(condition_2);
        config.getRouteRules().add(routeRule_2);
        
        String result = v2Convertor.transWebsiteConfiguration(config);

        logger.info(result);

        String assertStr = "<WebsiteConfiguration><IndexDocument><Suffix>suffix_test_transWebsiteConfiguration_without_redirectAllRequestsTo</Suffix></IndexDocument><ErrorDocument><Key>key_test_transWebsiteConfiguration_without_redirectAllRequestsTo</Key></ErrorDocument><RoutingRules><RoutingRule><Condition><KeyPrefixEquals>keyprefix_routeRule_1</KeyPrefixEquals><HttpErrorCodeReturnedEquals>405</HttpErrorCodeReturnedEquals></Condition><Redirect><HostName>www.routeRule_1.com</HostName><HttpRedirectCode>306</HttpRedirectCode><ReplaceKeyPrefixWith>replacekeyprefix_routeRule_1</ReplaceKeyPrefixWith><Protocol>http</Protocol></Redirect></RoutingRule><RoutingRule><Condition><KeyPrefixEquals>keyprefix_routeRule_2</KeyPrefixEquals><HttpErrorCodeReturnedEquals>404</HttpErrorCodeReturnedEquals></Condition><Redirect><HostName>www.routeRule_2.com</HostName><HttpRedirectCode>304</HttpRedirectCode><ReplaceKeyPrefixWith>replacekeyprefix_routeRule_2</ReplaceKeyPrefixWith><Protocol>https</Protocol></Redirect></RoutingRule></RoutingRules></WebsiteConfiguration>";

        assertEquals(assertStr, result);
    }
}
