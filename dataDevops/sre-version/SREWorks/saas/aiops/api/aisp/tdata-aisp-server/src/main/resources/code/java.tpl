import java.io.IOException;
import okhttp3.*;

public class Example {

    public String send() throws IOException {
        OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
        String body = {{ input }};
        Request request = new Request.Builder().url("{{ url }}")
            .addHeader("x-auth-app", "{{ app }}")       // 用自己的鉴权，替换x-auth-app字段。
            .addHeader("x-auth-key", "{{ key }}")       // 用自己的鉴权，替换x-auth-key字段。
            .addHeader("x-auth-user", "{{ user }}")     // 用自己的鉴权，替换x-auth-user字段。
            .addHeader("x-biz-app", "aiops,sreworks,prod")    
            .addHeader("x-auth-passwd", "{{ passwd }}") // 一天有效，请用自己的鉴权，替换x-auth-passwd字段！
            .post(RequestBody.create(MediaType.parse("application/json"), body))
            .build();
        Response response = httpClient.newCall(request).execute();
        assert response.body()!=null;
        return response.body().string();
    }
}
