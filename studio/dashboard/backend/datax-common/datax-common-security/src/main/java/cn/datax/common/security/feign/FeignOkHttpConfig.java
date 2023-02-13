package cn.datax.common.security.feign;

import feign.Feign;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@ConditionalOnClass(Feign.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignOkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                // 连接超时
                .connectTimeout(60, TimeUnit.SECONDS)
                // 读超时
                .readTimeout(60, TimeUnit.SECONDS)
                // 写超时
                .writeTimeout(60, TimeUnit.SECONDS)
                // 是否自动重连
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool())
                .build();
    }
}
