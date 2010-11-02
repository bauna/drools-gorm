package org.drools.gorm.session;

import java.util.Date;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.runtime.Environment;
import org.drools.runtime.process.ProcessInstance;

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

	void update();
}