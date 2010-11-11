package org.drools.gorm.processinstance;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.process.instance.event.SignalManager;
import org.drools.process.instance.event.SignalManagerFactory;

public class GormSignalManagerFactory implements SignalManagerFactory {

	public SignalManager createSignalManager(InternalKnowledgeRuntime kruntime) {
		return new GormSignalManager(kruntime);
	}

}
