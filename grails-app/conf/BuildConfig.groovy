grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        mavenRepo "http://repository.jboss.org/nexus/content/groups/public/"
        //mavenRepo "https://repository.jboss.org/nexus/content/repositories/public-jboss/"
        mavenRepo "http://people.apache.org/repo/m2-snapshot-repository/"
        //mavenRepo "http://people.apache.org/repo/m2-snapshot-repository/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        
        runtime 'org.jbpm:jbpm-bpmn2:5.1.0.Final'
        runtime ('org.drools:drools-persistence-jpa:5.2.0.Final') {
            transitive = false
        }
        runtime 'org.jbpm:jbpm-flow:5.1.0.Final'
        runtime ('org.jbpm:jbpm-persistence-jpa:5.1.0.Final') {
            transitive = false
        }
        
        runtime 'com.sun.xml.bind:jaxb-xjc:2.2.4'
        runtime 'com.sun.xml.bind:jaxb-impl:2.2.4'
        runtime 'javax.xml.stream:stax-api:1.0-2'
        
        runtime 'mysql:mysql-connector-java:5.1.11'
    }
}
