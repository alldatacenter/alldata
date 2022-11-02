package com.alibaba.sreworks.job.taskhandler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

@Data
@Slf4j
public class ApiContentAction {

    static HttpClient client = HttpClient.newBuilder().build();

    public static HttpResponse<String> run(ApiContent apiContent) throws IOException, InterruptedException {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(apiContent.getUrl()))
            .header("Content-Type", apiContent.getContentType());
        apiContent.headers().forEach(builder::header);
        switch (apiContent.getMethod()) {
            case GET:
                builder.GET();
                break;
            case PUT:
                builder.PUT(BodyPublishers.ofString(apiContent.getBody()));
                break;
            case POST:
                builder.POST(BodyPublishers.ofString(apiContent.getBody()));
                break;
            case DELETE:
                builder.DELETE();
                break;
        }
        return client.send(builder.build(), BodyHandlers.ofString());

    }

}
