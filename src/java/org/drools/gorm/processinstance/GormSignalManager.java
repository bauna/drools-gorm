package org.drools.gorm.processinstance;

import java.util.List;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.gorm.DomainUtils;
import org.jbpm.process.instance.event.DefaultSignalManager;

public class GormSignalManager extends DefaultSignalManager {

    public GormSignalManager(InternalKnowledgeRuntime kruntime) {
        super(kruntime);
    }
    
    public void signalEvent(String type,
                            Object event) {
        for ( long id : getProcessInstancesForEvent( type ) ) {
            getKnowledgeRuntime().getProcessInstance( id );
        }
        super.signalEvent( type,
                           event );
    }

    private List<Long> getProcessInstancesForEvent(String type) {
        return DomainUtils.ProcessInstancesWaitingForEvent(type);
    }

}