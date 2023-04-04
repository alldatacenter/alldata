package datart.core.data.provider;

import org.apache.commons.codec.digest.DigestUtils;

public class DataProviderUtils {

    public static String toCacheKey(DataProviderSource source, QueryScript queryScript, ExecuteParam param) {
        return DigestUtils.sha512Hex(String.join(":", source.getSourceId(),
                queryScript.toQueryKey(),
                param.toString()));
    }
}
