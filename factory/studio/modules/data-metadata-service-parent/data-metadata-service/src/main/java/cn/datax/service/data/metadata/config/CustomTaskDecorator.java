package cn.datax.service.data.metadata.config;


import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class CustomTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 获取主线程中的请求信息（我们的用户信息也放在里面）
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        attributes.setAttribute("token",((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest().getHeader("Authorization"),1);
        return () -> {
            try {
                // 将主线程的请求信息，设置到子线程中
                RequestContextHolder.setRequestAttributes(attributes);
                // 执行子线程，这一步不要忘了
                runnable.run();
            } finally {
                // 线程结束，清空这些信息，否则可能造成内存泄漏
                RequestContextHolder.resetRequestAttributes();
            }
        };
    }
}

