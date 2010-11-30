package org.drools.gorm.session;

import java.util.Date;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.process.instance.ProcessInstance;
import org.drools.runtime.Environment;

public interface ProcessInstanceInfo {
	long getId();

	String getProcessId();

	Date getStartDate();

	Date getLastModificationDate();

	Date getLastReadDate();

	void updateLastReadDate();

	int getState();

	ProcessInstance getProcessInstance(InternalKnowledgeRuntime kruntime,
			Environment env);
}