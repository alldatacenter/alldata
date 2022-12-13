package com.platform.dts.core.biz;

import com.platform.dts.core.biz.model.HandleCallbackParam;
import com.platform.dts.core.biz.model.HandleProcessCallbackParam;
import com.platform.dts.core.biz.model.RegistryParam;
import com.platform.dts.core.biz.model.ReturnT;

import java.util.List;

/**
 * @author AllDataDC 2022/11/27 21:52:49
 */
public interface AdminBiz {


    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);

    /**
     * processCallback
     *
     * @param processCallbackParamList
     * @return
     */
    ReturnT<String> processCallback(List<HandleProcessCallbackParam> processCallbackParamList);

    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registryRemove(RegistryParam registryParam);

}
