package com.alibaba.tdata.aisp.server.common.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * @ClassName: LogFilter
 * @Author: dyj
 * @DATE: 2021-04-01
 * @Description:
 **/
@Slf4j
@Component
public class AispLogFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            MDC.put(LogConstant.HTTP_EAGLEEYE_TRACE_ID, httpServletRequest.getHeader(LogConstant.HTTP_EAGLEEYE_TRACE_ID));

            if (httpServletRequest.getServletPath().equalsIgnoreCase("/status.taobao")) {
                chain.doFilter(httpServletRequest, httpServletResponse);
                return;
            }

            Map<String, String> requestMap = this
                .getTypesafeRequestMap(httpServletRequest);
            if (httpServletRequest.getContentLengthLong() <= 1000 && httpServletRequest.getContentLengthLong() != -1) {
                BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(httpServletRequest);
                String logMessage = "action=request||" +
                    "method=" +
                    httpServletRequest.getMethod() +
                    "||pathInfo=" +
                    httpServletRequest.getServletPath() +
                    "||requestParameters=" + requestMap +
                    "||body=" +
                    bufferedRequest.getRequestBody() +
                    "||remoteAddress=" +
                    httpServletRequest.getRemoteAddr();
                if (!StringUtils.isEmpty(bufferedRequest.getHeader("X-EmpId"))) {
                    logMessage += "||empId=" + bufferedRequest.getHeader("X-EmpId");
                }
                chain.doFilter(bufferedRequest, httpServletResponse);
                log.info(logMessage);
            } else {
                String logMessage = "action=request||" +
                    "method=" +
                    httpServletRequest.getMethod() +
                    "||pathInfo=" +
                    httpServletRequest.getServletPath() +
                    "||requestParameters=" + requestMap +
                    "||remoteAddress=" +
                    httpServletRequest.getRemoteAddr();
                if (!StringUtils.isEmpty(httpServletRequest.getHeader("X-EmpId"))) {
                    logMessage += "||empId=" + httpServletRequest.getHeader("X-EmpId");
                }
                chain.doFilter(httpServletRequest, httpServletResponse);
                log.info(logMessage);
            }
        } catch (Throwable ignored) {}
    }

    private Map<String, String> getTypesafeRequestMap(HttpServletRequest request) {
        Map<String, String> typesafeRequestMap = new HashMap<>();
        Enumeration<?> requestParamNames = request.getParameterNames();
        while (requestParamNames.hasMoreElements()) {
            String requestParamName = (String) requestParamNames.nextElement();
            String requestParamValue;
            if (requestParamName.equalsIgnoreCase("password")) {
                requestParamValue = "********";
            } else {
                requestParamValue = request.getParameter(requestParamName);
            }
            typesafeRequestMap.put(requestParamName, requestParamValue);
        }
        return typesafeRequestMap;
    }

    @Override
    public void destroy() {
    }

    private static final class BufferedRequestWrapper extends
        HttpServletRequestWrapper {

        private ByteArrayInputStream bais = null;
        private ByteArrayOutputStream baos = null;
        private BufferedServletInputStream bsis = null;
        private byte[] buffer = null;

        public BufferedRequestWrapper(HttpServletRequest req)
            throws IOException {
            super(req);
            // Read InputStream and store its content in a buffer.
            InputStream is = req.getInputStream();
            this.baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            int read;
            while ((read = is.read(buf)) > 0) {
                this.baos.write(buf, 0, read);
            }
            this.buffer = this.baos.toByteArray();
        }

        @Override
        public ServletInputStream getInputStream() {
            this.bais = new ByteArrayInputStream(this.buffer);
            this.bsis = new BufferedServletInputStream(this.bais);
            return this.bsis;
        }

        String getRequestBody() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                this.getInputStream()));
            String line = null;
            StringBuilder inputBuffer = new StringBuilder();
            do {
                line = reader.readLine();
                if (null != line) {
                    inputBuffer.append(line.trim());
                }
            } while (line != null);
            reader.close();
            return inputBuffer.toString().trim();
        }

    }

    private static final class BufferedServletInputStream extends
        ServletInputStream {

        private ByteArrayInputStream bais;

        public BufferedServletInputStream(ByteArrayInputStream bais) {
            this.bais = bais;
        }

        @Override
        public int available() {
            return this.bais.available();
        }

        @Override
        public int read() {
            return this.bais.read();
        }

        @Override
        public int read(byte[] buf, int off, int len) {
            return this.bais.read(buf, off, len);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }
}
