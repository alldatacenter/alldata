package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jialiang.tjl
 */
@Data
public final class Constant {
    /*** consumer数据的相关常量 */
    public final static int CONSUMER_NODE_HEARTBEAT_INVALID_TIME_INTERVAL = 60;
    public final static int CONSUMER_NODE_HEARTBEAT_TIME_INTERVAL = 10;

    public final static int CONSUMER_MYSQL_TABLE_EFFECTIVE_INTERVAL = 10;
    public static final int CONSUMER_ODPS_TABLE_EFFECTIVE_INTERVAL = 10;
    public final static int CONSUMER_LARK_DOCUMENT_EFFECTIVE_INTERVAL = 10;
    public final static int CONSUMER_SCRIPT_EFFECTIVE_INTERVAL = 10;
    public final static int CONSUMER_TT_READER_EFFECTIVE_INTERVAL = 10;

    public final static int SINGLE_CONSUMER_MYSQL_TABLE_CONCURRENT_EXEC_NUM = 100;
    public final static int SINGLE_CONSUMER_ODPS_TABLE_CONCURRENT_EXEC_NUM = 100;
    public final static int SINGLE_CONSUMER_LARK_DOCUMENT_CONCURRENT_EXEC_NUM = 100;
    public final static int SINGLE_CONSUMER_TT_READER_CONCURRENT_EXEC_NUM = 100;
    public final static int SINGLE_CONSUMER_SCRIPT_CONCURRENT_EXEC_NUM = 100;

    public final static String IMPORT_CONFIG_JSON_NODE_NAME_KEY = "__nodeName";
    public final static String WORLD_START_DATE = "1970-01-01 00:00:00";

    /*** meta配置的相关常量 */
    public final static String META_PREFIX = "__";
    public final static String INNER_TYPE = META_PREFIX + "type";
    public final static String INNER_ID = META_PREFIX + "id";
    public final static String INNER_GMT_CREATE = META_PREFIX + "gmt_create";
    public final static String INNER_GMT_MODIFIED = META_PREFIX + "gmt_modified";
    public final static String INNER_CONTENT = META_PREFIX + "content";

    public final static String AFTER_SEPARATOR = "__";
    public final static String SPECIAL_AFTER_SEPARATOR = "____";
    public final static String RELATION_META_SEPARATOR = "__";
    public final static String UPSERT_TIME_FIELD = "__upsertTimestamp";
    public final static String PARTITION_FIELD = "__partition";
    public final static String UNIQUE_INNER_ID = PARTITION_FIELD + INNER_ID;
    public final static List<String> NODE_RELATION_META = new ArrayList<>(Arrays.asList(INNER_ID, INNER_TYPE,
        UPSERT_TIME_FIELD, PARTITION_FIELD));

    /*** config default值 */
    public final static String DEFAULT_CATEGORY = META_PREFIX + "category";
    public final static String DEFAULT_NR_TYPE = META_PREFIX + "nrType";
    public final static String DEFAULT_NR_ID = META_PREFIX + "nrId";
    public final static String DEFAULT_NAME = META_PREFIX + "name";

    /*** index配置 */
    public final static long DEFAULT_INDEX_SIZE = 53687091200L;
    public final static int DEFAULT_INDEX_SHARDS = 10;
    public final static int DEFAULT_INDEX_REPLICAS = 1;
    public final static int DEFAULT_INDEX_FIELDS_LIMIT = 10000;
    public final static int DEFAULT_INDEX_MAX_RESULT_WINDOW = 100000000;
    public final static int ELASTICSEARCH_REINDEX_MAX_TIMES = 50;

    /*** 访问连接超时 */
    public final static int ELASTICSEARCH_READ_TIMEOUT = 120;
    public final static int ELASTICSEARCH_WRITE_TIMEOUT = 600;
    public final static int ELASTICSEARCH_CONNECT_TIMEOUT = 120;
    public final static int NEO4J_READ_TIMEOUT = 120;
    public final static int NEO4J_WRITE_TIMEOUT = 600;
    public final static int NEO4J_CONNECT_TIMEOUT = 120;

    /*** 导入配置 */
    public final static String DEFAULT_PARTITION = WORLD_START_DATE;
    public final static int NO_TTL = 0;
    public final static int DEFAULT_TTL = NO_TTL;
    public final static int DEFAULT_PARTITION_NUM = 10;
    public final static List<String> NULL_STRING = Arrays.asList("N/A", "\\N");
    public final static String ADJUST_JSONOBJECT_TO_ES = "pool";
    public final static int THREAD_POOL_CHANGE_RATE = 2;
    public final static int THREAD_POOL_MIN_SIZE = 1;
    public final static int ELASTICSEARCH_BULK_TIMEOUT_MINUTES = 30;
    public final static int ELASTICSEARCH_UPSERT_THREAD_POOL = 60;
    public final static int ELASTICSEARCH_UPSERTS_SIZE = 2000;
    public final static long RECOVERY_TIMESTAMP_MILLI_INTERVAL = 10000L;
    public final static int FETCH_DATA_SIZE = 1000;
    public final static int ELASTICSEARCH_LINES_TO_DATA_THREAD_POOL_SIZE = 2000;
    public final static int ELASTICSEARCH_LINES_TO_DATA_EACH_SIZE = 2000;

    public final static int RANDOM_WAIT_SECOND = 5 * 60;

    /*** elasticsearch index 创建相关参数 */
    public final static List<String> ELASTICSEARCH_INDEX_EX_ANALYZERS = Arrays.asList("english", "standard", "keyword");
    public final static String ELASTICSEARCH_INDEX_DEFAULT_TYPE = "nr";

    public final static String ELASTICSEARCH_INDEX_ANALYSIS = "{"
        + "    \"analyzer\": {"
        + "        \"full\": {"
        + "            \"tokenizer\": \"full\","
        + "            \"filter\": [\"lowercase\"]"
        + "        },"
        + "        \"pre\": {"
        + "            \"tokenizer\": \"pre\","
        + "            \"filter\": [\"lowercase\"]"
        + "        },"
        + "        \"suf\": {"
        + "            \"tokenizer\": \"keyword\","
        + "            \"filter\": [\"lowercase\", \"reverse\", \"suf\", \"reverse\"]"
        + "        },"
        + "        \"ik\": {"
        + "            \"tokenizer\": \"ik_smart\","
        + "            \"filter\": [\"lowercase\"]"
        + "        },"
        + "        \"whitespace\": {"
        + "            \"tokenizer\": \"whitespace\","
        + "            \"filter\": [\"lowercase\"]"
        + "        },"
        + "        \"lowercase\": {"
        + "            \"tokenizer\": \"keyword\","
        + "            \"filter\": [\"lowercase\"]"
        + "        }"
        + "    },"
        + "    \"filter\": {\n"
        + "        \"suf\": {\n"
        + "            \"type\": \"edge_ngram\",\n"
        + "            \"min_gram\": 1,\n"
        + "            \"max_gram\": 100\n"
        + "        }\n"
        + "    },"
        + "    \"tokenizer\": {"
        + "        \"full\": {"
        + "            \"type\": \"ngram\","
        + "            \"min_gram\": 1,"
        + "            \"max_gram\": 30,"
        + "            \"token_chars\": ["
        + "                \"letter\","
        + "                \"digit\","
        + "                \"punctuation\","
        + "                \"symbol\""
        + "            ]"
        + "         },"
        + "        \"pre\": {"
        + "            \"type\": \"edge_ngram\","
        + "            \"min_gram\": 1,"
        + "            \"max_gram\": 100,"
        + "            \"token_chars\": ["
        + "                \"letter\","
        + "                \"digit\","
        + "                \"punctuation\","
        + "                \"symbol\""
        + "            ]"
        + "         },"
        + "        \"suf\": {"
        + "            \"type\": \"edge_ngram\","
        + "            \"min_gram\": 1,"
        + "            \"max_gram\": 100,"
        + "            \"token_chars\": ["
        + "                \"letter\","
        + "                \"digit\","
        + "                \"punctuation\","
        + "                \"symbol\""
        + "            ]"
        + "         }"
        + "    }"
        + "}";
    public final static String ELASTICSEARCH_INDEX_DYNAMIC_TEMPLATES = "[]";

    public final static String ELASTICSEARCH_INDEX_PROPERTIES = "{}";

    /*** elasticsearch 查询相关配置 */
    public final static JSONArray ELASTICSEARCH_INDEX_SEARCH_MATCH_FIELDS =
        new JSONArray(new ArrayList<>(Arrays.asList(
            "*", "*.full", "*.lowercase^5", "*.keyword^10", "__id.lowercase^20", "__id.keyword^40", "__id.standard^35"
        )));
    public final static JSONArray ELASTICSEARCH_INDEX_SUGGEST_MATCH_FIELDS =
        new JSONArray(new ArrayList<>(Arrays.asList(
            "__id.full", "__id.pre^5", "__id.lowercase^10", "__id.keyword^20", "__id.standard^15"
        )));
    /**
     * 增加队列反压机制，设置队列最大长度
     */
    public static final int QUEUE_MAX_SIZE = 100000;
    /**
     * 增加队列提交睡眠机制，避免短时间提交大量数据
     */
    public static final long UPSERT_INTERVAL_IN_MILLIS = 100;
    public static final long UPSERT_SLEEP_IN_SECONDS = 30;

    public enum ComparePattern {
        // 小于
        lt,
        // 小于等于
        lte,
        // 大于
        gt,
        // 大于等于
        gte
    }

    public final static String CHAT_DEFAULT_CATEGORY = "chatDefaultCategory";
    public final static String CHAT_ROBOT_NAME = "DataOps";

    /*** redis缓存的各种key的值 */
    public final static String REDIS_REINDEX_STATUS = "reindexStatus";
    public final static String REDIS_REINDEX_NEWEST_UUID = "reindexNewestUuid";

    /*** graph db on es ***/
//    public final static String GRAPH_DB_INDEX_NAME = ".relation";

    public final static String BACKEND_STORE_PATTERNS_SPLIT_SYMBOL = ";";
    public final static int HASHMAP_INIT_CAPACITY = 1000;

    /*** 前端名称 ***/
    public final static String FRONTEND_NAME = "sreworks_productops_node";

    /*** var 类型 ***/
    public enum FRONTEND_VAR_TYPE {
        api, datasource, constant
    }

}
