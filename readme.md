This plugin integrates Drools 5.2 and jBPM 5 in a Grails application.
It provides persistent storage for Drools and jBPM 5 using GORM.

**Note: For Grails version before 1.2 use plugin version 0.3.**


# Usage
Currently, the plugin provides direct access to the Drools API.
(see [Drools Documentation Library](http://jboss.org/drools/documentation.html "Drools Documentation") and
[jBPM 5 Documentation Library](http://docs.jboss.org/jbpm/v5.0/userguide/ "jBPM 5 Documentation"))

For Grails usage examples you see the [integration tests on Github](https://github.com/bauna/drools-gorm/tree/master/test/integration "Integration Tests"))
or you can clone the [example application](https://github.com/bauna/drools-gorm-example "drools-gorm example app").


### Custom work item handler
You can use your Grails service artefact as work item handler if they implement the interface WorkItemHandler.

There are two possibilities to refer to your work item from workflow:

 *   _Convention:_ If you name your service class for example "HelloWorldWorkItemService" then the work item will be called "Hello World".
 *   Configuration: In your service class you can define a static variable called "workItemName".

### jBPM 5 Example

Create a KnowledgeBase:

    class JbpmService {
        static transactional = true

        /** The kstore is automatically declared by the drools-gorm plugin */
        def kstore

        /**
         * Returns a SessionConfiguration containing Drools services
         * implementations for Grails.
         *
         * @return a SessionConfiguration containing Drools services
         * implementations for Grails.
         */
        def getGORMSessionConfig() {
            Properties properties = new Properties();

            properties.setProperty("drools.commandService",
                    SingleSessionCommandService.class.getName());
            properties.setProperty("drools.processInstanceManagerFactory",
                    GormProcessInstanceManagerFactory.class.getName());
            properties.setProperty("drools.workItemManagerFactory",
                    GormWorkItemManagerFactory.class.getName());
            properties.setProperty("drools.processSignalManagerFactory",
                    GormSignalManagerFactory.class.getName());

            return new SessionConfiguration(properties);
        }

        /**
         * Construct a kbase with an example flow.
         *
         * @return a KnowledgeBase containing an example flow.
         */
        def KnowledgeBase createKbase() {
            KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

            kbuilder.add(ResourceFactory.newClassPathResource("helloWord.rf"),
                    ResourceType.BPMN2);

            if (kbuilder.hasErrors()) {
                throw new IllegalStateException("error compiling 'helloWord.rf':\n"
                        + kbuilder.errors);
            }

            return kbuilder.newKnowledgeBase();
        }

        /**
         * Start the a process.
         *
         * @param processId  the process id to start.
         * @return the id of the process instance.
         */
        def startProcess(String processId) {

            Environment env = KnowledgeBaseFactory.newEnvironment()

            StatefulKnowledgeSession ksession = kstore.newStatefulKnowledgeSession(createKbase(),
                    getGORMSessionConfig(),
                    env)

            ProcessInstance pi = ksession.startProcess(processId)

            def pid = pi.id
            ksession.dispose()
            return pid
        }
    }

### Development
* For issues, improvements or new features use [JIRA](http://jira.grails.org/browse/GPDROOLSGORM "Jira")
