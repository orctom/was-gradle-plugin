package com.orctom.gradle.was

import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.Sets
import com.orctom.was.model.WebSphereModel
import com.orctom.was.utils.PropertiesUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.bundling.Zip

/**
 * Abstract WAS Task
 * Created by hao on 11/9/15.
 */
class AbstractWASTask extends DefaultTask {

    String wasHome = System.getProperty('WAS_HOME')
    String applicationName
    String host = 'localhost'
    String port = '8880'
    String connectorType = 'SOAP'
    String cluster
    String cell
    String node
    String server
    String webservers
    String virtualHost
    String user
    String password
    String contextRoot
    String sharedLibs
    boolean parentLast = false
    boolean restartAfterDeploy = true
    boolean webModuleParentLast = false
    String deploymentsPropertyFile = "${project.projectDir.path}/was-gradle-plugin.properties"
    String packageFile
    String script
    String scriptArgs
    String javaoption
    String deployOptions
    boolean failOnError = false
    boolean verbose = false

    protected Set<WebSphereModel> getWebSphereModels() {
        setDefaultValues()
        String deployTargets = System.getProperty(Constants.KEY_DEPLOY_TARGETS)

        if (!Strings.isNullOrEmpty(deployTargets)) {
            File _deploymentsPropertyFile = new File(deploymentsPropertyFile)
            if (null != _deploymentsPropertyFile && _deploymentsPropertyFile.exists()) {
                Map<String, Properties> propertiesMap = PropertiesUtils.loadSectionedProperties(_deploymentsPropertyFile, getProjectProperties())
                if (null != propertiesMap && propertiesMap.size() >= 1) {
                    println "Multi targets: $deployTargets"
                    return getWebSphereModels(deployTargets, propertiesMap)
                }
            }

            if (null == deploymentsPropertyFile) {
                println "Property config file: $deploymentsPropertyFile not configured."
            }
            if (!_deploymentsPropertyFile.exists()) {
                println "Property config file: $deploymentsPropertyFile doesn't exist."
            }
            println 'Single target not properly configured.'
            return null
        } else {
            WebSphereModel model = getWebSphereModel()
            if (!model.isValid()) {
                println "Single target not properly configured. Missing 'cell' or 'cluster' or 'server' or 'node'"
                return null
            }
            println "Single target: ${model.host}"
            return Sets.newHashSet(model)
        }
    }

    protected void setDefaultValues() {
        if (Strings.isNullOrEmpty(applicationName)) {
            applicationName = "${project.name}-${project.version.toString()}"
        }
        if (Strings.isNullOrEmpty(packageFile)) {
            packageFile = getPackageFilePath()
        }
        if (!Strings.isNullOrEmpty(cluster)) {
            port = '8879'
        }
    }

    protected WebSphereModel getWebSphereModel() {
        WebSphereModel model = new WebSphereModel()
                .setWasHome(wasHome)
                .setApplicationName(applicationName)
                .setHost(host)
                .setPort(port)
                .setConnectorType(connectorType)
                .setCluster(cluster)
                .setCell(cell)
                .setNode(node)
                .setServer(server)
                .setWebservers(webservers)
                .setVirtualHost(virtualHost)
                .setContextRoot(contextRoot)
                .setSharedLibs(sharedLibs)
                .setParentLast(parentLast)
                .setWebModuleParentLast(webModuleParentLast)
                .setUser(user)
                .setPassword(password)
                .setPackageFile(new File(packageFile).getAbsolutePath())
                .setScript(script)
                .setScriptArgs(scriptArgs)
                .setJavaoption(javaoption)
                .setDeployOptions(deployOptions)
                .setFailOnError(failOnError)
                .setRestartAfterDeploy(restartAfterDeploy)
                .setVerbose(verbose)

        model.setProperties(getProjectProperties())

        return model
    }

    protected Set<WebSphereModel> getWebSphereModels(String deployTargetStr, Map<String, Properties> propertiesMap) {
        Set<String> deployTargets = new HashSet<String>()
        deployTargets.addAll(Splitter.on(",").split(deployTargetStr).asList())

        Set<WebSphereModel> models = new HashSet<WebSphereModel>()
        for (String deployTarget : deployTargets) {
            Properties props = propertiesMap.get(deployTarget)
            if (null == props || props.isEmpty()) {
                println "[SKIPPED] ${deployTarget}, not configured in property file."
                continue;
            }

            updateApplicationNameWithSuffix(props)

            WebSphereModel model = new WebSphereModel()
                    .setWasHome(wasHome)
                    .setApplicationName(getPropertyValue("applicationName", props))
                    .setHost(getPropertyValue("host", props))
                    .setPort(getPropertyValue("port", props))
                    .setConnectorType(getPropertyValue("connectorType", props))
                    .setCluster(getPropertyValue("cluster", props))
                    .setCell(getPropertyValue("cell", props))
                    .setNode(getPropertyValue("node", props))
                    .setServer(getPropertyValue("server", props))
                    .setWebservers(getPropertyValue("webservers", props))
                    .setVirtualHost(getPropertyValue("virtualHost", props))
                    .setContextRoot(getPropertyValue("contextRoot", props))
                    .setSharedLibs(getPropertyValue("sharedLibs", props))
                    .setParentLast(Boolean.valueOf(getPropertyValue("parentLast", props)))
                    .setWebModuleParentLast(Boolean.valueOf(getPropertyValue("webModuleParentLast", props)))
                    .setUser(getPropertyValue("user", props))
                    .setPassword(getPropertyValue("password", props))
                    .setPackageFile(new File(packageFile).getAbsolutePath())
                    .setScript(script)
                    .setScriptArgs(scriptArgs)
                    .setJavaoption(javaoption)
                    .setDeployOptions(deployOptions)
                    .setFailOnError(failOnError)
                    .setRestartAfterDeploy(Boolean.valueOf(getPropertyValue("restartAfterDeploy", props)))
                    .setVerbose(verbose)

            model.setProperties(props)
            if (model.isValid()) {
                models.add(model)
            }
        }

        return models
    }

    private void updateApplicationNameWithSuffix(Properties props) {
        String appNameSuffix = getPropertyValue("applicationNameSuffix", props)
        if (!Strings.isNullOrEmpty(appNameSuffix)) {
            String appName = getPropertyValue("applicationName", props)
            props.setProperty("applicationName", appName + "_" + appNameSuffix)
        }
    }

    protected String getPropertyValue(String propertyName, Properties props) {
        String value = props.getProperty(propertyName)
        if (isValueNotResolved(value)) {
            value = PropertiesUtils.resolve(value, props)
            props.setProperty(propertyName, value)
        }
        return value ? value.trim() : value
    }

    private isValueNotResolved = { String value ->
        return !Strings.isNullOrEmpty(value) && value.contains("{{") && value.contains("}}")
    }

    private Properties getProjectProperties() {
        Properties properties = new Properties(project.extensions.properties)
        setProperty(properties, "applicationName", applicationName)
        setProperty(properties, "host", host)
        setProperty(properties, "port", port)
        setProperty(properties, "connectorType", connectorType)
        setProperty(properties, "cluster", cluster)
        setProperty(properties, "cell", cell)
        setProperty(properties, "node", node)
        setProperty(properties, "server", server)
        setProperty(properties, "webservers", webservers)
        setProperty(properties, "virtualHost", virtualHost)
        setProperty(properties, "user", user)
        setProperty(properties, "password", password)
        setProperty(properties, "contextRoot", contextRoot)
        setProperty(properties, "sharedLibs", sharedLibs)
        setProperty(properties, "parentLast", String.valueOf(parentLast))
        setProperty(properties, "webModuleParentLast", String.valueOf(webModuleParentLast))
        setProperty(properties, "packageFile", new File(packageFile).getAbsolutePath())
        setProperty(properties, "javaoption", javaoption)
        setProperty(properties, "deployOptions", deployOptions)
        setProperty(properties, "failOnError", String.valueOf(failOnError))
        setProperty(properties, "script", script)
        setProperty(properties, "scriptArgs", scriptArgs)
        setProperty(properties, "verbose", String.valueOf(verbose))
        setProperty(properties, "restartAfterDeploy", String.valueOf(restartAfterDeploy))

        properties.setProperty("basedir", project.projectDir.path)
        properties.setProperty("project.basedir", project.projectDir.path)
        properties.setProperty("version", project.version.toString())
        properties.setProperty("project.version", project.version.toString())
        properties.setProperty("project.build.directory", project.buildDir.path)
        properties.setProperty("project.build.outputDirectory", "${project.buildDir.path}/classes")
        properties.setProperty("project.build.finalName", project.name)
        properties.setProperty("project.name", project.name)
        properties.setProperty("groupId", project.group.toString())
        properties.setProperty("project.groupId", project.group.toString())
        properties.setProperty("artifactId", project.name)
        properties.setProperty("project.artifactId", project.name)

        properties.setProperty("libsDir", project.properties.get('libsDir').toString())
        properties.setProperty("distsDir", project.properties.get('distsDir').toString())
        return properties
    }

    private static void setProperty(Properties properties, String key, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            properties.setProperty(key, value.trim())
        }
    }

    private String getPackageFilePath() {
        return project.tasks.withType(Zip).last().getArchivePath().path
    }

    @Override
    String getGroup() {
        'WAS (WebSphere)'
    }

    @Override
    String getDescription() {
        'Deploy a single war or ear to one or multi local or remote WebSphere Application Server (WAS)'
    }
}
