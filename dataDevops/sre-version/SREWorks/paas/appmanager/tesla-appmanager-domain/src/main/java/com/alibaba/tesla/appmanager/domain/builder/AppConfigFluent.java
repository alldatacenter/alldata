package com.alibaba.tesla.appmanager.domain.builder;

import java.util.List;

public interface AppConfigFluent<A extends AppConfigFluent<A>> extends Fluent<A> {

    AppConfigMeta getMetadata();

    A withMetadata(AppConfigMeta metadata);

    List<AppConfigComponent> getComponents();

    A withComponents(List<AppConfigComponent> components);

    A addToComponents(AppConfigComponent component);
}
