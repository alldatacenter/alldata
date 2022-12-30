package dynamicscripts

import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum
import com.alibaba.tesla.appmanager.domain.req.trait.TraitExecuteReq
import com.alibaba.tesla.appmanager.domain.res.trait.TraitExecuteRes
import com.alibaba.tesla.appmanager.trait.service.handler.TraitHandler
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * System Env Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
class TraitSystemEnv implements TraitHandler {

    private static final Logger log = LoggerFactory.getLogger(TraitSystemEnv.class)

    /**
     * 当前内置 Handler 类型
     */
    public static final String KIND = DynamicScriptKindEnum.TRAIT.toString()

    /**
     * 当前内置 Handler 名称
     */
    public static final String NAME = "systemEnv.trait.abm.io"

    /**
     * 当前内置 Handler 版本
     */
    public static final Integer REVISION = 2

    /**
     * Trait 业务侧逻辑执行
     *
     * @param request Trait 输入参数
     * @return Trait 修改后的 Spec 定义
     */
    @Override
    TraitExecuteRes execute(TraitExecuteReq request) {
        def spec = request.getSpec()
        def keys = spec.getJSONArray("keys")
        if (keys != null) {
            def envVars = new HashMap<String, Object>()
            for (Object key : keys) {
                String keyStr = (String) key;
                String value = System.getenv(keyStr);
                if (StringUtils.isEmpty(value)) {
                    value = "";
                }
                envVars.put(keyStr, value);
            }
            spec.put("env", envVars)
        }
        spec.put("annotations", request.getRef().getMetadata().getAnnotations())
        spec.put("labels", request.getRef().getMetadata().getLabels())
        return TraitExecuteRes.builder()
                .spec(spec)
                .build()
    }
}
