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
public class ComponentPackageTaskNextVersionRes implements Serializable {

    private static final long serialVersionUID = -7117635369995316694L;

    private String currentVersion;

    private String nextVersion;
}
