#!/usr/bin/env bash

teslaClientId=`cat /etc/tesla/config/config.yaml | grep teslaClientId`
teslaClientSecret=`cat /etc/tesla/config/config.yaml | grep teslaClientSecret`
teslaRedirectUri=`cat /etc/tesla/config/config.yaml | grep teslaRedirectUri`

teslaClientId=`echo ${teslaClientId#*:} | awk '$1=$1'`
teslaClientSecret=`echo ${teslaClientSecret#*:} | awk '$1=$1'`
teslaRedirectUri=`echo ${teslaRedirectUri#*:} | awk '$1=$1'`


if [[ "${teslaClientId}" != "" ]]; then
    sed -i 's/tesla.authPolicy=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.loginPolicy=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.logoutUrl=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties

    sed -i 's/tesla.oauth2UserAuthorizationUri=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.oauth2UserInfoUri=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.oauth2AccessTokenUri=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.oauth2ClientId=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.oauth2ClientSecret=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties
    sed -i 's/tesla.oauth2RedirectUri=.*//g' /home/admin/tesla/server/authproxy/resources/application-private.properties

    echo "
tesla.authPolicy=com.alibaba.tesla.authproxy.service.impl.AliyunAuthServiceManager
tesla.loginPolicy=com.alibaba.tesla.authproxy.interceptor.AliyunLoginInterceptor
tesla.logoutUrl=https://account.aliyun.com/logout/logout.htm
tesla.oauth2UserAuthorizationUri=https://signin.aliyun.com/oauth2/v1/auth
tesla.oauth2UserInfoUri=https://oauth.aliyun.com/v1/userinfo
tesla.oauth2AccessTokenUri=https://oauth.aliyun.com/v1/token
tesla.oauth2ClientId=${teslaClientId}
tesla.oauth2ClientSecret=${teslaClientSecret}
tesla.oauth2RedirectUri=${teslaRedirectUri}
    " >> /home/admin/tesla/server/authproxy/resources/application-private.properties


fi
/opt/taobao/java/bin/java -jar /home/admin/tesla/server/authproxy/bin/tesla-authproxy.jar --spring.config.location=/home/admin/tesla/server/authproxy/resources/ --spring.profiles.active=private -Xmx128m