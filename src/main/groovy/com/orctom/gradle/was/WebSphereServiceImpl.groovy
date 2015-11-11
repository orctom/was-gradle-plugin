package com.orctom.gradle.was

import com.orctom.was.model.Command
import com.orctom.was.model.WebSphereModel
import com.orctom.was.service.impl.AbstractWebSphereServiceImpl
import org.gradle.api.Project

/**
 * Deploy to was
 * Created by hao on 11/11/15.
 */
class WebSphereServiceImpl extends AbstractWebSphereServiceImpl{

    private Project project

    WebSphereServiceImpl(WebSphereModel model, String workingDir, Project project) {
        super(model, workingDir)
        this.project = project
    }

    @Override
    protected void executeCommand(Command command) {
        project.exec {
            commandLine = ['echo', 'hello world']
        }
    }
}
