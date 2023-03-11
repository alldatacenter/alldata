package com.platform.flink;

import lombok.*;

@Data
@AllArgsConstructor
@Setter
@Getter
public class UrlVisitBy {
    private long wStart;
    private long wEnd;
    private String userId;
    private long uv;
    private long pv;
    private String url;
}
