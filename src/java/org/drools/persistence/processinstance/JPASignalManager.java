package org.drools.persistence.processinstance;

import java.util.List;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.gorm.DomainUtils;
import org.drools.process.instance.event.DefaultSignalManager;

public class JPASignalManager extends DefaultSignalManager {

    public JPASignalManager(InternalKnowledgeRuntime kruntime) {
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