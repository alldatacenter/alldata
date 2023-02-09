package com.obs.test.buckets;

import java.util.ArrayList;
import java.util.List;

import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCorsRule;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.LifecycleConfiguration.NoncurrentVersionTransition;
import com.obs.services.model.LifecycleConfiguration.Rule;
import com.obs.services.model.LifecycleConfiguration.Transition;
import com.obs.services.model.StorageClassEnum;

import com.obs.test.UserInfo;
import com.obs.test.tools.BucketTools;

public abstract class BaseBucketTest {

    protected String generateS3PolicyString(String bucketName, String action, String userId) {
        return "{" + "\"Version\":\"2008-10-17\"," + "\"Id\":\"Policy1584074750088\"," + "\"Statement\":[" + "    {"
                + "        \"Sid\":\"Customized1584072051364\"," + "        \"Effect\":\"Allow\","
                + "        \"Principal\":{" + "            \"AWS\":[\"arn:aws:iam::" + userId + "\"]" + "        },"
                + "        \"Action\":[\"s3:" + action + "\"]," + "        \"Resource\":[\"arn:aws:s3:::" + bucketName
                + "\"]" + "    }" + "]}";
    }

    protected void setModifyObjectMetaDataPolicy(UserInfo srcUser, UserInfo destUser, String bucketName) {
        // String policy =
        // "{"
        // + "\"Statement\":["
        // + " {"
        // + " \"Sid\":\"Customized1584072051364\","
        // + " \"Effect\":\"Allow\","
        // + " \"Principal\":{"
        // + " \"ID\":[\"domain/" + destUser.getOwnerId() + ":user/" +
        // destUser.getDomainId() + "\"]"
        // + " },"
        // + " \"Action\":[\"ModifyObjectMetaData\"],"
        // + " \"Resource\":[\"" + bucketName + "/*\"]" + " }"
        // + "]}";

        // 采用S3协议
        String policy = "{" + "\"Version\":\"2008-10-17\"," + "\"Id\":\"Policy1584074750088\"," + "\"Statement\":["
                + "    {" + "        \"Sid\":\"Customized1584072051364\"," + "        \"Effect\":\"Allow\","
                + "        \"Principal\":{" + "            \"AWS\":[\"arn:aws:iam::" + destUser.getDomainId() + ":user/"
                + destUser.getOwnerId() + "\"]" + "        }," + "        \"Action\":[\"s3:ModifyObjectMetaData\"],"
                + "        \"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]" + "    }" + "]}";

        BucketTools.setBucketPolicy(srcUser.getObsClient(), bucketName, policy);
    }

    /**
     * 设置桶策略，使其具备设置桶请求者付费的权限
     * 
     * @param srcUser
     * @param destUser
     * @param bucketName
     */
    protected void setBucketRequestPaymentPolicy(UserInfo srcUser, UserInfo destUser, String bucketName) {
        // 采用S3协议
        String policy = "{" + "\"Version\":\"2008-10-17\"," + "\"Id\":\"Policy1584074750088\"," + "\"Statement\":["
                + "    {" + "        \"Sid\":\"Customized1584072051364\"," + "        \"Effect\":\"Allow\","
                + "        \"Principal\":{" + "            \"AWS\":[\"arn:aws:iam::" + destUser.getDomainId() + ":user/"
                + destUser.getOwnerId() + "\"]" + "        },"
                + "        \"Action\":[\"s3:GetBucketRequestPayment\",\"s3:PutBucketRequestPayment\"],"
                + "        \"Resource\":[\"arn:aws:s3:::" + bucketName + "\"]" + "    }" + "]}";

        BucketTools.setBucketPolicy(srcUser.getObsClient(), bucketName, policy);
    }

    /**
     * 设置桶策略，使其具备设置桶请求者付费的权限
     * 
     * @param srcUser
     * @param destUser
     * @param bucketName
     */
    protected void setAllBucketPolicy(UserInfo srcUser, UserInfo destUser, String bucketName) {
        // 采用S3协议
        String policy = "{" + "\"Version\":\"2008-10-17\"," + "\"Id\":\"Policy1584074750088\"," + "\"Statement\":["
                + "    {" + "        \"Sid\":\"Customized1584072051364\"," + "        \"Effect\":\"Allow\","
                + "        \"Principal\":{" + "            \"AWS\":[\"arn:aws:iam::" + destUser.getDomainId() + ":user/"
                + destUser.getOwnerId() + "\"]" + "        }," + "        \"Action\":[\"s3:*\"],"
                + "        \"Resource\":[\"arn:aws:s3:::" + bucketName + "\"]" + "    },"

                + "    {" + "        \"Sid\":\"Customized1584072051365\"," + "        \"Effect\":\"Allow\","
                + "        \"Principal\":{" + "            \"AWS\":[\"arn:aws:iam::" + destUser.getDomainId() + ":user/"
                + destUser.getOwnerId() + "\"]" + "        }," + "        \"Action\":[\"s3:*\"],"
                + "        \"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]" + "    }"

        + "]}";

        BucketTools.setBucketPolicy(srcUser.getObsClient(), bucketName, policy);
    }

    /**
     * 清理所有桶策略
     * 
     * @param user
     * @param bucketName
     */
    protected void deleteBucketPolicy(UserInfo user, String bucketName) {
        BucketTools.deleteBucketPolicy(user.getObsClient(), bucketName);
    }

    /**
     * 设置桶的ACL
     * 
     * @param obsClient
     * @param bucketName
     * @param objectKey
     */
    protected void setBucketAcl(UserInfo srcUser, UserInfo destUser, String bucketName) {
        BucketTools.setBucketAcl(srcUser.getObsClient(), srcUser.getDomainId(), destUser.getDomainId(), bucketName,
                true, false);
    }

    /**
     * 生成一个LifecycleConfiguration
     * 
     * @return
     */
    protected LifecycleConfiguration createLifecycleConfiguration() {
        LifecycleConfiguration config = new LifecycleConfiguration();
        Rule rule = config.new Rule();
        rule.setEnabled(true);
        rule.setId("rule1");
        rule.setPrefix("prefix");
        Transition transition = config.new Transition();
        // 指定满足前缀的对象创建30天后转换
        transition.setDays(29);
        // 指定对象转换后的存储类型
        transition.setObjectStorageClass(StorageClassEnum.COLD);
        // 直接指定满足前缀的对象转换日期
        // transition.setDate(new
        // SimpleDateFormat("yyyy-MM-dd").parse("2018-10-31"));
        rule.getTransitions().add(transition);

        NoncurrentVersionTransition noncurrentVersionTransition = config.new NoncurrentVersionTransition();
        // 指定满足前缀的对象成为历史版本30天后转换
        noncurrentVersionTransition.setDays(30);
        // 指定历史版本对象转换后的存储类型
        noncurrentVersionTransition.setObjectStorageClass(StorageClassEnum.COLD);
//        rule.getNoncurrentVersionTransitions().add(noncurrentVersionTransition);

        config.addRule(rule);

        return config;
    }

    /**
     * 创建一个BucketCors 对象
     * 
     * @return
     */
    protected BucketCors createSetBucketCorsRequest() {
        BucketCors cors = new BucketCors();

        List<BucketCorsRule> rules = new ArrayList<BucketCorsRule>();
        BucketCorsRule rule = new BucketCorsRule();

        ArrayList<String> allowedOrigin = new ArrayList<String>();
        // 指定允许跨域请求的来源
        allowedOrigin.add("http://www.a.com");
        allowedOrigin.add("http://www.b.com");
        rule.setAllowedOrigin(allowedOrigin);

        ArrayList<String> allowedMethod = new ArrayList<String>();
        // 指定允许的跨域请求方法(GET/PUT/DELETE/POST/HEAD)
        allowedMethod.add("GET");
        allowedMethod.add("HEAD");
        allowedMethod.add("PUT");
        rule.setAllowedMethod(allowedMethod);

        ArrayList<String> allowedHeader = new ArrayList<String>();
        // 控制在OPTIONS预取指令中Access-Control-Request-Headers头中指定的header是否被允许使用
        allowedHeader.add("x-obs-header");
        rule.setAllowedHeader(allowedHeader);

        ArrayList<String> exposeHeader = new ArrayList<String>();
        // 指定允许用户从应用程序中访问的header
        exposeHeader.add("x-obs-expose-header");
        rule.setExposeHeader(exposeHeader);

        // 指定浏览器对特定资源的预取(OPTIONS)请求返回结果的缓存时间,单位为秒
        rule.setMaxAgeSecond(10);
        rules.add(rule);
        cors.setRules(rules);

        return cors;
    }
}
