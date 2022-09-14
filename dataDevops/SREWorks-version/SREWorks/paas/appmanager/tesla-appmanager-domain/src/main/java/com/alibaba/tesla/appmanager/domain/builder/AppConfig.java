package com.alibaba.tesla.appmanager.domain.builder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    private AppConfigMeta metadata;

    private List<AppConfigComponent> components;
}
