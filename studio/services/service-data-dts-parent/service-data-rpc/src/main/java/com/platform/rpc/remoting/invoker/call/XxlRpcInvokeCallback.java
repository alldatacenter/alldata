package com.platform.rpc.remoting.invoker.call;


/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * @Description:
 **/
public abstract class XxlRpcInvokeCallback<T> {

    public abstract void onSuccess(T result);

    public abstract void onFailure(Throwable exception);


    // ---------------------- thread invoke callback ----------------------

    private static ThreadLocal<XxlRpcInvokeCallback> threadInvokerFuture = new ThreadLocal<XxlRpcInvokeCallback>();

    /**
     * get callback
     *
     * @return
     */
    public static XxlRpcInvokeCallback getCallback() {
        XxlRpcInvokeCallback invokeCallback = threadInvokerFuture.get();
        threadInvokerFuture.remove();
        return invokeCallback;
    }

    /**
     * set future
     *
     * @param invokeCallback
     */
    public static void setCallback(XxlRpcInvokeCallback invokeCallback) {
        threadInvokerFuture.set(invokeCallback);
    }

    /**
     * remove future
     */
    public static void removeCallback() {
        threadInvokerFuture.remove();
    }


}
