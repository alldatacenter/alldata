package com.alibaba.tesla.appmanager.domain.builder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigComponentScope {

    private AppConfigComponentScopeRef scopeRef = new AppConfigComponentScopeRef();
}
