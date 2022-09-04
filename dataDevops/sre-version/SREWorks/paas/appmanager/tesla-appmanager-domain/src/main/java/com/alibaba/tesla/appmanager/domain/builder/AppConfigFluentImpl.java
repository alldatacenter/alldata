package com.alibaba.tesla.appmanager.domain.builder;

import java.util.ArrayList;
import java.util.List;

public class AppConfigFluentImpl<A extends AppConfigFluent<A>>
        extends BaseFluent<A>
        implements AppConfigFluent<A> {

    private AppConfigMetaBuilder metadata;
    private List<AppConfigComponentBuilder> components;

    public AppConfigFluentImpl() {
    }

    @Override
    public AppConfigMeta getMetadata() {
        return metadata != null ? metadata.build() : null;
    }

    @Override
    public A withMetadata(AppConfigMeta metadata) {
        if (metadata != null) {
            this.metadata = new AppConfigMetaBuilder(metadata);
        }
        return (A) this;
    }

    @Override
    public List<AppConfigComponent> getComponents() {
        return build(this.components);
    }

    @Override
    public A withComponents(List<AppConfigComponent> components) {
        if (components != null) {
            this.components = new ArrayList<>();
            for (AppConfigComponent item : components) {
                this.components.add(new AppConfigComponentBuilder(item));
            }
        } else {
            this.components = null;
        }
        return (A) this;
    }

    @Override
    public A addToComponents(AppConfigComponent instance) {
        if (this.components == null) {
            this.components = new ArrayList<>();
        }
        this.components.add(new AppConfigComponentBuilder(instance));
        return (A) this;
    }
}
