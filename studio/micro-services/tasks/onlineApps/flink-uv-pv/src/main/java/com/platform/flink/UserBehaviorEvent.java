package com.platform.flink;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserBehaviorEvent {
    private Integer userId;
    private Integer itemId;
    private String category;
    private String clientIp;
    private String action;
    private Long ts;
}
