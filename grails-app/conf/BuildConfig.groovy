import grails.util.Environment

/*************************************************************************
 * tranSMART - translational medicine data mart
 *
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 *
 * This product includes software developed at Janssen Research & Development, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.    You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.    You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 ******************************************************************/


def forkSettingsRun = [
        minMemory: 1536,
        maxMemory: 4096,
        maxPerm:   384,
        debug:     false,
]
def forkSettingsOther = [
        minMemory: 256,
        maxMemory: 1024,
        maxPerm:   384,
        debug:     false,
]

grails.project.fork = [
        test:    [ *:forkSettingsOther, daemon: true ],
        run:     forkSettingsRun,
        war:     forkSettingsRun,
        console: forkSettingsOther ]

//grails.plugin.location.'rdc-rmodules' = "../Rmodules"

grails.project.war.file = "target/${appName}.war"

/* we need at least servlet-api 2.4 because of HttpServletResponse::setCharacterEncoding */
grails.servlet.version = "2.5"

grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn"
    repositories {
        grailsCentral()
        mavenCentral()
        //Note: External configuration should contain
        //either hyve or transmartfoundation repository
    }
    dependencies {
        // you can remove whichever you're not using
        runtime 'org.postgresql:postgresql:9.3-1100-jdbc4'
        runtime 'com.oracle:ojdbc7:12.1.0.1'

        compile 'org.transmartproject:transmart-core-api:1.0-SNAPSHOT'
        compile 'net.sf.opencsv:opencsv:2.3'
        compile "org.apache.lucene:lucene-core:2.4.0"
        compile "org.apache.lucene:lucene-demos:2.4.0"
        compile "org.apache.lucene:lucene-highlighter:2.4.0"
        compile 'commons-net:commons-net:3.3' // used for ftp transfers
        compile 'org.apache.commons:commons-math:2.2' //>2MB lib briefly used in ChartController
        compile 'org.codehaus.groovy:http-builder:0.4.1', {
            excludes 'groovy', 'nekohtml'
        }
        compile 'org.grails:grails-plugin-rest:2.3.5-hyve4'
        compile 'org.transmartproject:transmart-core-api:1.0-SNAPSHOT'

        /* we need at least servlet-api 2.4 because of HttpServletResponse::setCharacterEncoding */
        compile "javax.servlet:servlet-api:$grails.servlet.version" /* delete from the WAR afterwards */

        /* for GeneGo web services: */
        compile 'axis:axis:1.4'

        /* for SAML authentication
         * see example config at https://github.com/thehyve/transmartApp/commit/bf15bb51a#all_commit_comments
         * Note that the config namespace changed from grails.plugins.springsecurity to
         * grails.plugin.springsecurity though (plugins -> plugin) */
        compile('org.springframework.security.extensions:spring-security-saml2-core:1.0.0.RC3-f0fb87a') {
            //excludes of spring securirty necessary because they are for an older version (3.1 branch)
            //also remove xercesImpl because it breaks tomcat and is not otherwise needed
            excludes 'spring-security-config', 'spring-security-core', 'spring-security-web', 'xercesImpl'
        }

        runtime 'org.javassist:javassist:3.16.1-GA'

        
        test('junit:junit:4.11') {
            transitive = false /* don't bring hamcrest */
            export     = false
        }

        test 'org.hamcrest:hamcrest-core:1.3',
             'org.hamcrest:hamcrest-library:1.3'

        test 'org.gmock:gmock:0.9.0-r435-hyve2', {
            transitive = false
        }
    }

    plugins {
        build ':release:3.0.1'
        build ':rest-client-builder:1.0.3'
        build ':tomcat:7.0.47'

        compile ':build-info:1.2.5'
        compile ':hibernate:3.6.10.7'
        compile ':quartz:1.0-RC2'
        compile ':rdc-rmodules:0.3-SNAPSHOT'
        compile ':spring-security-core:2.0-RC2'
        compile ":spring-security-oauth2-provider:1.0.5.2"

        runtime ':prototype:1.0'
        runtime ':jquery:1.7.1'
        runtime ':transmart-core:1.0-SNAPSHOT'
        runtime ':resources:1.2.1'
        runtime ':transmart-mydas:0.1-SNAPSHOT'
        runtime ':dalliance-plugin:0.2-SNAPSHOT'
        runtime ':transmart-rest-api:0.1-SNAPSHOT'

        // Doesn't work with forked tests yet
        //test ":code-coverage:1.2.6"
        test ':transmart-core-db-tests:1.0-SNAPSHOT'
    }
}

grails.war.resources = { stagingDir ->
    delete(file: "${stagingDir}/WEB-INF/lib/servlet-api-${grails.servlet.version}.jar")
}

// Use new NIO connector in order to support sendfile
grails.tomcat.nio = true

def buildConfigFile = new File("${userHome}/.grails/${appName}Config/" +
        "BuildConfig.groovy")
if (buildConfigFile.exists()) {
    println "Processing external build config at $buildConfigFile"

    def slurpedBuildConfig = new ConfigSlurper(Environment.current.name).
            parse(buildConfigFile.toURL())

    /* For development, it's interesting to use the plugins in-place.
     * This allows the developer to put the grails.plugin.location.* assignments
     * in an out-of-tree BuildConfig file if they want to.
     * Snippet from https://gist.github.com/acreeger/910438
     */
    slurpedBuildConfig.grails.plugin.location.each { String k, v ->
        if (!new File(v).exists()) {
            println "WARNING: Cannot load in-place plugin from ${v} as that " +
                    "directory does not exist."
        } else {
            println "Loading in-place plugin $k from $v"
            grails.plugin.location."$k" = v
        }
        if (grailsSettings.projectPluginsDir?.exists()) {
            grailsSettings.projectPluginsDir.eachDir { dir ->
                // remove optional version from inline definition
                def dirPrefix = k.replaceFirst(/:.+/, '') + '-'
                if (dir.name.startsWith(dirPrefix)) {
                    println "WARNING: Found a plugin directory at $dir that is a " +
                            "possible conflict and may prevent grails from using " +
                            "the in-place $k plugin."
                }
            }
        }
    }

    /* dependency resolution in external BuildConfig */
    Closure originalDepRes = grails.project.dependency.resolution;
    if (slurpedBuildConfig.grails.project.dependency.resolution) {
        Closure extraDepRes = slurpedBuildConfig.grails.project.dependency.resolution;
        grails.project.dependency.resolution = {
            originalDepRes.delegate        = extraDepRes.delegate        = delegate
            originalDepRes.resolveStrategy = extraDepRes.resolveStrategy = resolveStrategy
            originalDepRes.call(it)
            extraDepRes.call(it)
        }
    }
}

// vim: set et sw=4 ts=4:
