package com.hw.security.flink;

import com.alibaba.fastjson2.JSON;
import com.hw.security.flink.enums.DataMaskType;
import com.hw.security.flink.policy.DataMaskPolicy;
import com.hw.security.flink.policy.RowFilterPolicy;
import com.hw.security.flink.util.ResourceReader;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * The manager of row-level filter and data masking policies,which can be connected to the policies in ranger later.
 *
 * @author: HamaWhite
 */
public class PolicyManager {

    private static final String DATA_MASK_TYPES_FILE = "data_mask_types.json";

    private final List<RowFilterPolicy> rowFilterPolicyList;

    private final List<DataMaskPolicy> dataMaskPolicyList;

    private final List<DataMaskType> maskTypeList;

    public PolicyManager() {
        this.rowFilterPolicyList = new LinkedList<>();
        this.dataMaskPolicyList = new LinkedList<>();

        try {
            byte[] bytes = ResourceReader.readFile(DATA_MASK_TYPES_FILE);
            this.maskTypeList = JSON.parseArray(new String(bytes), DataMaskType.class);
        } catch (Exception e) {
            throw new SecurityException(String.format("read file %s error", DATA_MASK_TYPES_FILE), e);
        }
    }

    public Optional<String> getRowFilterCondition(String username, String catalogName, String database, String tableName) {
        for (RowFilterPolicy policy : rowFilterPolicyList) {
            if (policy.getUsername().equals(username)
                    && policy.getCatalogName().equals(catalogName)
                    && policy.getDatabase().equals(database)
                    && policy.getTableName().equals(tableName)) {
                return Optional.ofNullable(policy.getCondition());
            }
        }
        return Optional.empty();
    }

    public Optional<String> getDataMaskCondition(String username, String catalogName, String database, String tableName
            , String columnName) {
        for (DataMaskPolicy policy : dataMaskPolicyList) {
            if (policy.getUsername().equals(username)
                    && policy.getCatalogName().equals(catalogName)
                    && policy.getDatabase().equals(database)
                    && policy.getTableName().equals(tableName)
                    && policy.getColumnName().equals(columnName)) {
                return Optional.ofNullable(policy.getCondition());
            }
        }
        return Optional.empty();
    }


    public DataMaskType getDataMaskType(String typeName) {
        DataMaskType ret = null;
        for (DataMaskType maskType : maskTypeList) {
            if (StringUtils.equals(maskType.getName(), typeName)) {
                ret = maskType;
                break;
            }
        }
        return ret;
    }

    public boolean addPolicy(RowFilterPolicy policy) {
        return rowFilterPolicyList.add(policy);
    }

    public boolean removePolicy(RowFilterPolicy policy) {
        return rowFilterPolicyList.remove(policy);
    }

    public boolean addPolicy(DataMaskPolicy policy) {
       return dataMaskPolicyList.add(policy);
    }

    public boolean removePolicy(DataMaskPolicy policy) {
        return dataMaskPolicyList.remove(policy);
    }
}
