package top.omooo.logger;

/**
 * Created by Omooo
 * Date:2019/4/1
 * 栈帧工具类
 * 用于输出当前错误日志出错的类名以及行数
 */
public class StackTraceUtil {
    private static StackTraceElement getStackTraceElement() {
        StackTraceElement result = null;
        boolean shouldTrace = false;
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : elements) {
            boolean isLogMethod = element.getClassName().equals(StackTraceUtil.class.getName());
            if (shouldTrace && !isLogMethod) {
                result = element;
                break;
            }
            shouldTrace = isLogMethod;
        }
        return result;
    }

    public static String getStackTrace() {
        StackTraceElement element = getStackTraceElement();
        if (element != null) {
            return element.getClassName() + "." + element.getMethodName() + " (" + element.getFileName() + ":" + element.getLineNumber() + ")";
        } else {
            return null;
        }
    }
}
