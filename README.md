# was-gradle-plugin [![Build Status](https://api.travis-ci.org/orctom/was-gradle-plugin.png)](https://travis-ci.org/orctom/was-gradle-plugin)

**This plugin is the gradle version of [was-maven-plugin](https://github.com/orctom/was-maven-plugin)**

- [Introduction](#introduction)
- [Installation](#installation)
- [Usage](#usage)
	- [Single Target Server](#single-target-server)
	- [Multi Target Servers](#multi-target-servers)
- [Customized Jython Script File](#customized-jython-script-file)
- [Continues Deployment with Jenkins](#continues-deployment-with-jenkins)
- [With Global Security Turned on](#with-global-security-turned-on)
- [Change List](#change-list)
	- [1.0](#10)

## Introduction
Gradle plugin to deploy a single war or ear to one or multi local or remote WebSphere Application Server (WAS) at a single build.  
Tested on WAS 8.5  
**Requires: WebSphere Application Server installation on host box! But no need to be configured, nor running.**

**Requires: JDK 7 or later**

### Parameters
| Name						| Type		| Description																								    |
| ------------------------- | --------- | ------------------------------------------------------------------------------------------------------------- |
| **wasHome**				| String	| WebSphere Application Server home. Default: `${env.WAS_HOME}`, **required**								    |
| **applicationName**		| String	| Application name displayed in admin console. Default: `${project.build.finalName}`						    |
| applicationNameSuffix		| String	| Will be appended to applicationName, as `applicationName_applicationNameSuffix`, **property file only** 	    |
| host						| String	| Local/Remote WAS IP/domain URL. e.g. `10.95.0.100`, `devtrunk01.company.com`, default: `localhost`   		    |
| port						| String	| Default: `8879` (when `cluster` not empty); `8880` (when `cluster` empty)									    |
| connectorType 			| String	| Default: `SOAP` 																							    |
| cluster					| String	| Target cluster name, **required** if target WAS is a cluster	    										    |
| cell						| String	| Target cell name																							    |
| node						| String	| Target node name,												 											    |
| server					| String	| Target server name,									            										    |
| webservers				| String	| Target web server(s) name, comma-separated.																    |
| virtualHost				| String	| Target virtual host name																					    |
| user						| String	| Account username for **target WAS** admin console, if global security is turned on						    |
| password					| String	| Account password for **target WAS** admin console, if global security is turned on						    |
| contextRoot				| String	| **required** for war deployment                   														    |
| sharedLibs				| String	| Bind the exist shared libs to ear/war, comma-separated (,)												    |
| parentLast				| Boolean	| `true` to set classloader mode of application to `PARENT_LAST`, default `false`							    |
| restartAfterDeploy		| Boolean	| `true` to restart server after deploy, `false` to start application directly. Default `true`				    |
| webModuleParentLast		| Boolean	| `true` to set classloader mode of web module to `PARENT_LAST`, default `false`							    |
| **packageFile**			| String	| The EAR/WAR package that will be deployed to remote RAS, Default: `${project.artifact.file}`				    |
| **failOnError**			| Boolean	| Default: `false` Whether failed the build when failed to deploy.                          				    |
| **verbose**				| Boolean	| Whether show more detailed info in log																	    |
| **script**				| String	| Your own jython script for deployment. Double braces for variables, such as: `{{cluster}}`                    |
| **scriptArgs**			| String	| Args that will be passed to the `script`                                          	                        |
| **javaoption**			| String	| Sample `-Xmx1024m`, `-Xms512m -Xmx1024m`                                          	                        |
| deployOptions				| String	| Sample `-precompileJSPs`, `-precompileJSPs -deployws`                                	                        |
| **preSteps**				| Ant tasks	| Ant tasks that can be executed before the deployments														    |
| **postSteps**				| Ant tasks	| Ant tasks that can be executed after the deployments														    |
| deploymentsPropertyFile	| File		| Configuring above params (except those in **bold**) for multi targets. Default: `was-gradle-plugin.properties`|

Generally, you need to specify at least
 * `cluster` for a cluster
 * `server` and `node` for a non-cluster

## Installation
### Gradle 2.0 or older
```groovy
buildscript {
	repositories {
		maven {
			url "https://plugins.gradle.org/m2/"
		}
	}
	dependencies {
		classpath "gradle.plugin.com.orctom.gradle.was:was-gradle-plugin:1.0"
	}
}

apply plugin: "com.orctom.was"
```

### Gradle 2.1 or newer
```groovy
plugins {
	id "com.orctom.was" version "1.0"
}
```

## Usage
### Single Target Server
```groovy
was {
	wasHome = ${env.WAS_HOME}
	applicationName = ${project.build.finalName}
	host = localhost
	server = server01
	node = node01
	virtualHost = default_host
	verbose = true
}
```

```
gradle build was
```

### Multi Target Servers
Please create `was-gradle-plugin.properties` at your project root, and put configurations into it.
The section name (inside the spare-brackets, such as `dev-trunk`) will be used to identify the target WAS.

And you don't have to have `was {...}` section configured in build.gradle in this case.

```properties
[DEFAULT]
virtualHost=default_host

[dev-trunk1]
host=devtrunk1.company.com
applicationNameSuffix=trunk1
cluster=cluster01
server=server01

[dev-trunk2]
host=devtrunk2.company.com
applicationNameSuffix=trunk2
cluster=cluster02
server=server02

[dev-trunk3]
host=devtrunk3.company.com
applicationNameSuffix=trunk3
cluster=cluster03
server=server03
virtualHost=devtrunk3_host
```

**Deploy to `dev-trunk1` and `dev-trunk2`**
```
gradle was -Ddeploy_targets=`dev-trunk1`,`dev-trunk2`
```
**Deploy to `dev-trunk2` and `dev-trunk3`**
```
gradle was -Ddeploy_targets=`dev-trunk2`,`dev-trunk3`
```

## Customized Jython Script File
This plugin also supports customized jython script if you need to tween the installation options, such server mappings.

You can copy-create it from [the built-in one](https://github.com/orctom/was-util/blob/master/src/main/resources/jython/websphere.py),
or write a totally different one of you own.

Double braces for variables, such as: `{{cluster}}`, properties in was-gradle-plugin.properties are all available as variables.

## Continues Deployment with Jenkins
We could move this plugin to a profile, and utilize [Extended Choice Parameter plugin](https://wiki.jenkins-ci.org/display/JENKINS/Extended+Choice+Parameter+plugin) to make this parameterized.

#### Sample Jenkins Job Configuration
**Configure**

![Jenkins Job configure](https://raw.github.com/orctom/was-gradle-plugin/master/screenshots/configure.png "Jenkins Job Configure")

**Trigger**

![Jenkins Job Trigger](https://raw.github.com/orctom/was-gradle-plugin/master/screenshots/trigger.png "Jenkins Job Trigger")

## With Global Security Turned on
When Global Security is enabled on remote WAS (not under a same deployment manager), certificates of remote WAS need to be added to local trust store. 
We could configure WAS to prompt to add them to local trust store.
 1. Open ${WAS_HOME}/properties/ssl.client.props 
 2. Change the value of `com.ibm.ssl.enableSignerExchangePrompt` to `gui` or `stdin`

* `gui`: will prompt a Java based window, this requires a X window installed. 
* `stdin`: when using ssh, or on client linux without X window installed. 

## Change List

#### 1.0
* Fixed server mapping issue with cluster. Apps will be deployed to all servers that managed by the specified cluster.
* Web servers support, use `webservers` to specify the web server(s) that you want to bind. (but you still have to 'update web server plug-in configuration' by your self.)
* Added parameter `deployOptions`, expecting space-separated options such as `-precompileJSPs -deployws`, which will be prepended in the deployment options.
* Fixed issue about `failOnError`.
* Extract some common code including `websphere.py` to [was-util](https://github.com/orctom/was-util), which is also been used by [was-gradle-plugin](https://github.com/orctom/was-gradle-plugin)

