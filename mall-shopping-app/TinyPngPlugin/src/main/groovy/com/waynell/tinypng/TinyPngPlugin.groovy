/**
 * Created by Wayne Yang
 * Copyright (c) 2015-present, mogujie.
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 */

package com.waynell.tinypng

import org.gradle.api.Plugin
import org.gradle.api.Project

public class TinyPngPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("tinyInfo", TinyPngExtension);

        project.afterEvaluate {
            project.task("tinyPng", type: TinyPngTask)
        }
    }
}