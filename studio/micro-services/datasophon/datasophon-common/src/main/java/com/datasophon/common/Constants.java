package com.datasophon.common;

import com.datasophon.common.utils.PropertyUtils;

import java.util.regex.Pattern;

/**
 * Constants
 */
public final class Constants {

    public static final String INSTALL_PATH = PropertyUtils.getString("install.path");
    public static final String DATA = "data";
    public static final String INSTALL_TYPE = "install_type";
    public static final String TOTAL = "total";

    public static final String DATASOPHON = "datasophon";

    public static final String HOST_MAP = "_host_map";
    public static final String COMMAND_ID = "command_id";
    public static final String COMMAND_HOST_ID = "command_host_id";
    public static final String HOST_MD5 = "_host_md5";
    public static final String ID_RSA = "/.ssh/id_rsa";
    public static final String HOSTNAME = "hostname";

    public static final String MASTER_MANAGE_PACKAGE_PATH = INSTALL_PATH + "/DDP/packages";
    public static final String UNZIP_DDH_WORKER_CMD = "tar -zxvf " + INSTALL_PATH + "/datasophon-worker.tar.gz -C " + INSTALL_PATH;
    public static final String START_DDH_WORKER_CMD = "service datasophon-worker restart";

    public static final String WORKER_PACKAGE_NAME = "datasophon-worker.tar.gz";
    public static final String WORKER_SCRIPT_PATH = INSTALL_PATH + "/datasophon-worker/script/";
    public static final String WORKER_PATH = INSTALL_PATH + "/datasophon-worker";

    public static final String IP_HOST = "ip_host";
    public static final String HOST_IP = "host_ip";
    public static final String FRAME_ID = "frame_id";
    public static final String SERVICE_ID = "service_id";
    public static final String CLUSTER_ID = "cluster_id";
    public static final String MANAGED = "managed";
    public static final String SERVICE_ROLE_TYPE = "service_role_type";
    public static final String JSON = "json";
    public static final String CONFIG = "_config";
    public static final String SERVICE_ROLE_HOST_MAPPING = "service_role_host_mapping";
    public static final String UNDERLINE = "_";
    public static final String HOST_SERVICE_ROLE_MAPPING = "host_service_role_mapping";
    public static final String DETAILS_USER_ID = "user_id";
    public static final String MASTER = "master";
    public static final String CONFIG_FILE = "_config_file";
    public static final String QUERY = "query";
    public static final String SUCCESS = "success";
    public static final String FRAME_CODE = "frameCode";
    public static final String FRAME_VERSION = "frameVersion";
    public static final String SCRIPT = "script";
    public static final String SERVICE_NAME = "service_name";
    public static final String SERVICE_ROLE_STATE = "service_role_state";
    public static final String LOCALE_LANGUAGE = "language";
    public static final String CODE = "code";
    public static final String CLUSTER_CODE = "cluster_code";
    public static final String ID = "id";
    public static final String START_DISTRIBUTE_AGENT = "start_distribute_agent";
    public static final String CHECK_WORKER_MD5_CMD = "md5sum "+INSTALL_PATH+"/datasophon-worker.tar.gz | awk '{print $1}'";
    public static final String CREATE_TIME = "create_time";
    public static final String COMMAND_TYPE = "command_type";
    public static final String SERVICE_ROLE_NAME = "service_role_name";
    public static final String FRAME_CODE_1 = "frame_code";
    public static final String UPDATE_COMMON_CMD = "sh "+INSTALL_PATH+"/datasophon-worker/script/sed_common.sh ";
    public static final String MASTER_HOST = "masterHost";
    public static final String MASTER_WEB_PORT = "masterWebPort";

    public static final String HOST_COMMAND_ID = "host_command_id";

    public static final String CONFIG_VERSION = "config_version";
    public static final String HAS_EN = ".*[a-zA-z].*";
    public static final String ALERT_TARGET_NAME = "alert_target_name";
    public static final String USER_INFO = "userInfo";
    public static final String CUSTOM = "custom";
    public static final String INPUT = "input";
    public static final String SERVICE_ROLE_JMX_MAP = "service_role_jmx_port";
    public static final String MULTIPLE = "multiple";
    public static final String CLUSTER_STATE = "cluster_state";
    public static final String PATH = "path";
    public static final String SERVICE_INSTANCE_ID = "service_instance_id";
    public static final String IS_ENABLED = "is_enabled";
    public static final String SORT_NUM = "sort_num";
    public static final String CN = "chinese";
    public static final String ALERT_GROUP_ID = "alert_group_id";
    public static final String ALERT_QUOTA_NAME = "alert_quota_name";
    public static final String NAME = "name";
    public static final String QUEUE_NAME = "queue_name";
    public static final String ROLE_TYPE = "role_type";
    public static final String CLUSTER_FRAME = "cluster_frame";
    public static final String VARIABLE_NAME = "variable_name";
    public static final String SERVICE_ROLE_INSTANCE_ID = "service_role_instance_id";
    public static final String X86JDK = "jdk-8u333-linux-x64.tar.gz";
    public static final String ARMJDK = "jdk-8u333-linux-aarch64.tar.gz";
    public static final String COMMAND_STATE = "command_state";
    public static final String ROLE_GROUP_ID = "role_group_id";
    public static final String ROLE_GROUP_TYPE = "role_group_type";
    public static final String NEET_RESTART = "need_restart";

    public static final String ALERT_GROUP_NAME = "alert_group_name";
    public static final String ALERT_LEVEL = "alert_level";
    public static final String CPU_ARCHITECTURE = "cpu_architecture";
    public static final String HOST_STATE = "host_state";
    public static final String FAILED = "failed";
    public static final String SERVICE_CATEGORY = "service_category";
    public static final String ROLE_GROUP_NAME = "role_group_name";
    public static final String NODE_LABEL = "node_label";
    public static final String GROUP_ID = "group_id";
    public static final String USER_ID = "user_id";
    public static final String GROUP_NAME = "group_name";
    public static final String RACK = "rack";
    public static final String SERVICE_STATE = "service_state";

    private Constants() {
        throw new IllegalStateException("Constants Exception");
    }

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";
    /**
     * session user
     */
    public static final String SESSION_USER = "session.user";

    public static final String SESSION_ID = "sessionId";
    /**
     * session timeout
     */
    public static final int SESSION_TIME_OUT = 7200;

    /**
     * http header
     */
    public static final String HTTP_HEADER_UNKNOWN = "unKnown";

    /**
     * http X-Forwarded-For
     */
    public static final String HTTP_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * http X-Real-IP
     */
    public static final String HTTP_X_REAL_IP = "X-Real-IP";

    /**
     * UTF-8
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * user name regex
     */
    public static final Pattern REGEX_USER_NAME = Pattern.compile("^[a-zA-Z0-9._-]{3,39}$");
    /**
     * comma ,
     */
    public static final String COMMA = ",";

    /**
     * slash /
     */
    public static final String SLASH = "/";


    /**
     * SPACE " "
     */
    public static final String SPACE = " ";

    /**
     * SINGLE_SLASH /
     */
    public static final String SINGLE_SLASH = "/";
    /**
     * status
     */
    public static final String STATUS = "status";

    /**
     * message
     */
    public static final String MSG = "msg";

    public static final String REGEX_VARIABLE = "\\$\\{(.*?)\\}";


    /**
     * email regex
     */
    public static final Pattern REGEX_MAIL_NAME = Pattern.compile("^([a-z0-9A-Z]+[_|\\-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$");

    /**
     * 常量-数值100
     */
    public static final int ONE_HUNDRRD = 100;

    /**
     * 常量-数值200
     */
    public static final int TWO_HUNDRRD = 200;

    /**
     * 常量-数值10
     */
    public static final int TEN= 10;

    /**
     * 常量-zkserver
     */
    public static final String ZKSERVER= "zkserver";

    public static final String  CENTER_BRACKET_LEFT= "[";

    public static final String   CENTER_BRACKET_RIGHT= "]";
    /**
     * 常量-连接号
     */
    public static final String   HYPHEN= "-";

    public static final String TASK_MANAGER = "taskmanager";
    public static final String JOB_MANAGER = "jobmanager";
    public static final String x86_64 = "x86_64";
    public static final String PROMETHEUS = "prometheus";

    public static final String XML = "xml";
    public static final String PROPERTIES = "properties";
    public static final String PROPERTIES2 = "properties2";
    public static final String PROPERTIES3 = "properties3";

    /**
     * os name properties
     */
    public static final String OSNAME_PROPERTIES = "os.name";

    /**
     * windows os name
     */
    public static final String OSNAME_WINDOWS = "Windows";

    /**
     * windows hosts file basedir
     */
    public static final String WINDOWS_HOST_DIR = "C:/Windows/System32/drivers";
    /**
     * root user
     */
    public static final String   ROOT= "root";
}
