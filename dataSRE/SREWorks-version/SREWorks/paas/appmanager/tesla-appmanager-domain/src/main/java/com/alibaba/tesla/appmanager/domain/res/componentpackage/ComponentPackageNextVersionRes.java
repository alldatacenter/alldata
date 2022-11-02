package com.alibaba.tesla.appmanager.domain.res.componentpackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentPackageNextVersionRes implements Serializable {

    private static final long serialVersionUID = -1814270470884418688L;

    private String currentVersion;

    private String nextVersion;
}
