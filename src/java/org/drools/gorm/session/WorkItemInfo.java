package org.drools.gorm.session;

import java.util.Date;

import org.drools.process.instance.WorkItem;
import org.drools.runtime.Environment;

public interface WorkItemInfo extends HasBlob<Long> {
    
	String getName();

	Date getCreationDate();

	long getProcessInstanceId();

	long getState();

	WorkItem getWorkItem(Environment env);

}