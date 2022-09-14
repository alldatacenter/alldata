package com.alibaba.tesla.tkgone.server.domain.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author xueyong.zxy
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackendStoreDTO {
    private String name;
    private String type;
    private String schema;
    private String host;
    private Integer port;
    private String user;
    private String password;
    private JSONObject indexPatterns;
    private Boolean defaultStore;
    private Integer shardPerNode;
    @JSONField(serialize = false)
    private Map<String, JSONObject> indices;
    @JSONField(serialize = false)
    private String uri;
    private String backupStore;

    @JSONField(name = "index_patterns")
    public JSONObject getIndexPatterns() {
        return this.indexPatterns;
    }

    @JSONField(name = "index_patterns")
    public void setIndexPatterns(JSONObject indexPatterns) {
        this.indexPatterns = indexPatterns;
    }

    @JSONField(name = "default_store")
    public Boolean isDefaultStore() {
        if (null == defaultStore) {
            return false;
        }
        return this.defaultStore;
    }

    @JSONField(name = "default_store")
    public void setDefaultStore(Boolean defaultStore) {
        if (null == defaultStore) {
            this.defaultStore = false;
        }
        this.defaultStore = defaultStore;
    }

    @JSONField(name = "shard_per_node")
    public Integer getShardPerNode() {
        return this.shardPerNode;
    }

    @JSONField(name = "shard_per_node")
    public void setShardPerNode(Integer shardPerNode) {
        this.shardPerNode = shardPerNode;
    }

    public Map<Pattern, JSONObject> getIndexPatternMap() {
        if (null == this.indexPatterns) {
            return new HashMap<>();
        }
        Map<Pattern, JSONObject> result = new HashMap<>();
        for (String key : this.indexPatterns.keySet()) {
            Pattern pattern = Pattern.compile(key);
            result.put(pattern, this.indexPatterns == null ? null : this.indexPatterns.getJSONObject(key));
        }
        return result;
    }
}
