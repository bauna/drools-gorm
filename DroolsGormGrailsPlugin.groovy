import grails.util.Environment;

class DroolsGormGrailsPlugin {
    // the plugin version
    def version = "0.5.4"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = (Environment.current != Environment.TEST) ? 
            [ "grails-app/views/error.gsp", "grails-app/domain/org/drools/gorm/test/DroolsTest.groovy" ] :
            [ "grails-app/views/error.gsp"]

    def author = "Pablo 'bauna' Nussembaum, Diego López León"
    def authorEmail = "baunax@gmail.com, dieguitoll@gmail.com"
    def title = "Drools Gorm"
    def description = '''\\
Drools 5.2 and jBPM 5 integration plugin. 
It provides persistent storage for Drools and jBPM 5.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/drools-gorm"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        kstore(org.drools.gorm.impl.KnowledgeStoreServiceImpl) {}
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
