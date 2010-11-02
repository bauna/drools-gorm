package org.drools.gorm.session;

import java.util.Date;

import javax.persistence.PreUpdate;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.runtime.Environment;
import org.drools.runtime.process.ProcessInstance;

public interface ProcessInstanceInfo {

	String getProcessId();

	Date getStartDate();

	Date getLastModificationDate();

	Date getLastReadDate();

	void updateLastReadDate();

	int getState();

	ProcessInstance getProcessInstance(InternalKnowledgeRuntime kruntime,
			Environment env);

	@PreUpdate
	void update();

}