package org.drools.gorm.processinstance;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.process.instance.WorkItemManager;
import org.drools.process.instance.WorkItemManagerFactory;

public class GormWorkItemManagerFactory implements WorkItemManagerFactory {

	public WorkItemManager createWorkItemManager(InternalKnowledgeRuntime kruntime) {
		return new GormWorkItemManager(kruntime);
	}

}
