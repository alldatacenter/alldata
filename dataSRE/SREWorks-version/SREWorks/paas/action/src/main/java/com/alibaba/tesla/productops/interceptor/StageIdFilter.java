package com.alibaba.tesla.productops.interceptor;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

@WebFilter(urlPatterns = "/*", filterName = "StageIdFilter")
@Component
public class StageIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {

        // 防止流读取一次后就没有了, 所以需要将流继续写出去
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        // 这里将原始request传入，读出流并存储
        ServletRequest request = new MyHttpServletRequest(httpServletRequest);
        // 这里将原始request替换为包装后的request，此后所有进入controller的request均为包装后的
        filterChain.doFilter(request, servletResponse);//

    }

}
