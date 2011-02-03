package org.drools.gorm.processinstance;

import org.drools.common.InternalKnowledgeRuntime;
import org.jbpm.process.instance.ProcessInstanceManager;
import org.jbpm.process.instance.ProcessInstanceManagerFactory;

public class GormProcessInstanceManagerFactory implements ProcessInstanceManagerFactory {

	public ProcessInstanceManager createProcessInstanceManager(InternalKnowledgeRuntime kruntime) {
		return new GormProcessInstanceManager(kruntime);
	}

}
