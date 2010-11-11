package org.drools.gorm.session;

import java.util.Date;

import org.drools.process.instance.WorkItem;

public interface WorkItemInfo {

	String getName();

	Date getCreationDate();

	long getProcessInstanceId();

	long getState();

	WorkItem getWorkItem();

	void update();

	Long getId();

	void setId(Long id);
}