package com.orctom.gradle.was

import com.google.common.base.Throwables
import com.orctom.was.model.WebSphereModel
import com.orctom.was.model.WebSphereServiceException
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WASDeployTask extends AbstractWASTask {

    @TaskAction
    void start() {
        getLogger().info(Constants.PLUGIN_ID + " - deploy");
        Set<WebSphereModel> models = getWebSphereModels()
        if (null == models || models.isEmpty()) {
            println '[SKIPPED DEPLOYMENT] empty target server configured, please check your configuration'
            return
        }

        final def workingDir = "${project.buildDir}/was-gradle-plugin/py/"

        if (models.size() > 1) {
            int numOfProcessors = Runtime.getRuntime().availableProcessors()
            int poolSize = models.size() > numOfProcessors ? numOfProcessors : models.size()
            ExecutorService executor = Executors.newFixedThreadPool(poolSize)
            models.each { model ->
                executor.execute(new Runnable() {
                    @Override
                    void run() {
                        execute(model, workingDir)
                    }
                })
            }
        } else {
            execute(models.first(), workingDir)
        }
    }

    void execute(WebSphereModel model, String workingDir) {
        try {
            getLogger().info('======================    deploy    ========================')
            new WebSphereServiceImpl(model, workingDir, project).deploy()
        } catch (Throwable t) {
            if (model.failOnError) {
                throw new WebSphereServiceException(t);
            } else {
                getLogger().error("##############  Exception occurred during deploying to WebSphere  ###############");
                getLogger().error(Throwables.getStackTraceAsString(t));
            }
        }
    }
}
