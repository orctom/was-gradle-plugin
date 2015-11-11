package com.orctom.gradle.was

import org.gradle.api.Plugin
import org.gradle.api.Project

class WASPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('was', type: WASDeployTask)
    }
}