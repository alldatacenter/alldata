package com.platform.mall.filter;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * @author wulinhao
 * @ClassName：CustomExceptionFilter
 * @Description：dubbo自定义异常处理
 */
public class CustomExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException, ValidateException {
        Result result = invoker.invoke(invocation);
        if (!result.hasException()) {
            return result;
        }

        Throwable e = result.getException();
        if (e instanceof ValidateException) {
            throw new ValidateException(e.getMessage());
        } else {
            e.printStackTrace();
            throw new RpcException(wrapperExceptionMessage(e));
        }
    }

    private String wrapperExceptionMessage(Throwable throwable) {
        StringBuilder result = new StringBuilder("");

        // Print our stack trace
        result.append(throwable);

        StackTraceElement[] trace = throwable.getStackTrace();
        if (null != trace && trace.length > 0) {
            for (StackTraceElement traceElement : trace) {
                result.append("<br/>" + " at " + traceElement);
            }
        }

        // Print suppressed exceptions, if any
        Throwable[] suppresseds = throwable.getSuppressed();
        if (null != suppresseds && suppresseds.length > 0) {
            for (Throwable se : suppresseds) {
                result.append("<br/>" + " [CIRCULAR REFERENCE:" + throwable + "]");
            }
        }

        // Print cause, if any
        Throwable ourCause = throwable.getCause();
        if (ourCause != null) {
            result.append("<br/>" + " [CIRCULAR REFERENCE:" + ourCause + "]");
        }

        return result.toString();
    }
}
