package top.omooo.tinypng_plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class TinyPngPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create("tinyInfo", TinyPngExtension)

        project.afterEvaluate {
            project.task("tinyTask", type: TinyPngTask)
        }
    }
}