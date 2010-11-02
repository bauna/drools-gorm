package org.drools.gorm.session;

public interface ProcessInstanceEventInfo {

	long getId();

	long getProcessInstanceId();

	String getEventType();

}