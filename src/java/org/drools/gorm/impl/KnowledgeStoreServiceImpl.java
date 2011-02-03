package org.drools.gorm.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.drools.KnowledgeBase;
import org.drools.SessionConfiguration;
import org.drools.command.CommandService;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.gorm.processinstance.GormProcessInstanceManagerFactory;
import org.drools.gorm.processinstance.GormSignalManagerFactory;
import org.drools.gorm.processinstance.GormWorkItemManagerFactory;
import org.drools.gorm.session.SingleSessionCommandService;
import org.drools.persistence.jpa.KnowledgeStoreService;
import org.drools.persistence.jpa.JpaJDKTimerService;
import org.jbpm.process.instance.ProcessInstanceManagerFactory;
import org.drools.process.instance.WorkItemManagerFactory;
import org.jbpm.process.instance.event.SignalManagerFactory;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.time.TimerService;

// => drools-persistence-jpa->
//    org.drools.persistence.jpa.impl.KnowledgeStoreServiceImpl
public class KnowledgeStoreServiceImpl 
    implements
    KnowledgeStoreService {

//    private static final Logger log = LoggerFactory.getLogger(KnowledgeStoreServiceImpl.class);
    
    private Class<? extends CommandExecutor>               commandServiceClass;
    private Class<? extends WorkItemManagerFactory>        workItemManagerFactoryClass;
    private Class<? extends TimerService>                  timerServiceClass;

    private Properties                                     configProps = new Properties();

    public KnowledgeStoreServiceImpl() {
        setDefaultImplementations();
    }

    protected void setDefaultImplementations() {
        setCommandServiceClass( SingleSessionCommandService.class );
        setProcessInstanceManagerFactoryClass( GormProcessInstanceManagerFactory.class );
        setWorkItemManagerFactoryClass( GormWorkItemManagerFactory.class );
        setProcessSignalManagerFactoryClass( GormSignalManagerFactory.class );
        setTimerServiceClass( JpaJDKTimerService.class );
    }

    public StatefulKnowledgeSession newStatefulKnowledgeSession(KnowledgeBase kbase,
                                                                KnowledgeSessionConfiguration configuration,
                                                                Environment environment) {
        if ( configuration == null ) {
            configuration = new SessionConfiguration();
        }

        if ( environment == null ) {
            throw new IllegalArgumentException( "Environment cannot be null" );
        }

        return new CommandBasedStatefulKnowledgeSession( (CommandService) buildCommanService( kbase,
                                                                             mergeConfig( configuration ),
                                                                             environment ) );
    }

    public StatefulKnowledgeSession loadStatefulKnowledgeSession(int id, KnowledgeBase kbase,
            KnowledgeSessionConfiguration configuration, Environment environment) {
        if (configuration == null) {
            configuration = new SessionConfiguration();
        }

        if (environment == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        return new CommandBasedStatefulKnowledgeSession((CommandService) buildCommanService(id,
                kbase, 
                mergeConfig(configuration), 
                environment));
    }

    private CommandExecutor buildCommanService(int sessionId,
                                              KnowledgeBase kbase,
                                              KnowledgeSessionConfiguration conf,
                                              Environment env) {

        try {
            Class< ? extends CommandExecutor> serviceClass = getCommandServiceClass();
            Constructor< ? extends CommandExecutor> constructor = serviceClass.getConstructor( int.class,
                                                                                              KnowledgeBase.class,
                                                                                              KnowledgeSessionConfiguration.class,
                                                                                              Environment.class );
            return constructor.newInstance( sessionId,
                                            kbase,
                                            conf,
                                            env );
        } catch ( SecurityException e ) {
            throw new IllegalStateException( e );
        } catch ( NoSuchMethodException e ) {
            throw new IllegalStateException( e );
        } catch ( IllegalArgumentException e ) {
            throw new IllegalStateException( e );
        } catch ( InstantiationException e ) {
            throw new IllegalStateException( e );
        } catch ( IllegalAccessException e ) {
            throw new IllegalStateException( e );
        } catch ( InvocationTargetException e ) {
            throw new IllegalStateException( e );
        }
    }

    private CommandExecutor buildCommanService(KnowledgeBase kbase,
                                              KnowledgeSessionConfiguration conf,
                                              Environment env) {

        Class< ? extends CommandExecutor> serviceClass = getCommandServiceClass();
        try {
            Constructor< ? extends CommandExecutor> constructor = serviceClass.getConstructor( KnowledgeBase.class,
                                                                                              KnowledgeSessionConfiguration.class,
                                                                                              Environment.class );
            return constructor.newInstance( kbase,
                                            conf,
                                            env );
        } catch ( SecurityException e ) {
            throw new IllegalStateException( e );
        } catch ( NoSuchMethodException e ) {
            throw new IllegalStateException( e );
        } catch ( IllegalArgumentException e ) {
            throw new IllegalStateException( e );
        } catch ( InstantiationException e ) {
            throw new IllegalStateException( e );
        } catch ( IllegalAccessException e ) {
            throw new IllegalStateException( e );
        } catch ( InvocationTargetException e ) {
            throw new IllegalStateException( e );
        }
    }

    private KnowledgeSessionConfiguration mergeConfig(KnowledgeSessionConfiguration configuration) {
        ((SessionConfiguration) configuration).addProperties( configProps );
        return configuration;
    }

    public int getStatefulKnowledgeSessionId(StatefulKnowledgeSession ksession) {
        if ( ksession instanceof CommandBasedStatefulKnowledgeSession ) {
            SingleSessionCommandService commandService = (SingleSessionCommandService) ((CommandBasedStatefulKnowledgeSession) ksession).getCommandService();
            return commandService.getSessionId();
        }
        throw new IllegalArgumentException( "StatefulKnowledgeSession must be an a CommandBasedStatefulKnowledgeSession" );
    }

    public void setCommandServiceClass(Class< ? extends CommandExecutor> commandServiceClass) {
        if ( commandServiceClass != null ) {
            this.commandServiceClass = commandServiceClass;
            configProps.put( "drools.commandService",
                             commandServiceClass.getName() );
        }
    }

    public Class< ? extends CommandExecutor> getCommandServiceClass() {
        return commandServiceClass;
    }

    public void setTimerServiceClass(Class< ? extends TimerService> timerServiceClass) {
        if ( timerServiceClass != null ) {
            this.timerServiceClass = timerServiceClass;
            configProps.put( "drools.timerService",
            		         timerServiceClass.getName() );
        }
    }

    public Class< ? extends TimerService> getTimerServiceClass() {
        return timerServiceClass;
    }

    public void setProcessInstanceManagerFactoryClass(Class<? extends ProcessInstanceManagerFactory> 
    		processInstanceManagerFactoryClass) {
        configProps.put( "drools.processInstanceManagerFactory",
                         processInstanceManagerFactoryClass.getName() );
    }

    public void setWorkItemManagerFactoryClass(Class< ? extends WorkItemManagerFactory> clazz) {
        if ( clazz != null ) {
            this.workItemManagerFactoryClass = clazz;
            configProps.put( "drools.workItemManagerFactory",
                             clazz.getName() );
        }
    }

    public Class< ? extends WorkItemManagerFactory> getWorkItemManagerFactoryClass() {
        return workItemManagerFactoryClass;
    }

    public void setProcessSignalManagerFactoryClass(Class<? extends SignalManagerFactory> clazz) {
        configProps.put( "drools.processSignalManagerFactory",
                         clazz.getName() );
    }
}
