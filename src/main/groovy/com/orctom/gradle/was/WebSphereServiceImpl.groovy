package com.orctom.gradle.was

import com.orctom.was.model.Command
import com.orctom.was.model.WebSphereModel
import com.orctom.was.service.impl.AbstractWebSphereServiceImpl
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

/**
 * Deploy to was
 * Created by hao on 11/11/15.
 */
class WebSphereServiceImpl extends AbstractWebSphereServiceImpl {

    private Project project

    WebSphereServiceImpl(WebSphereModel model, String workingDir, Project project) {
        super(model, workingDir)
        this.project = project
    }

    @Override
    protected void executeCommand(Command command) {
        ExecResult result = project.exec { ExecSpec exec ->
            exec.executable = command.executable
            exec.workingDir = command.workingDir
            exec.args = args = command.argsAsList
            exec.standardInput = System.in
            exec.standardOutput = System.out
            exec.errorOutput = System.out
        }
        if (result.exitValue != 0) {
            new RuntimeException("Failed to execute, recturn code ${exitVal}")
        }
    }
}
