package org.drools.gorm.session;

import java.util.Date;

import org.drools.gorm.session.marshalling.GORMSessionMarshallingHelper;

public interface SessionInfo {

	int getId();

	void setMarshallingHelper(GORMSessionMarshallingHelper helper);

	byte[] getData();

	Date getStartDate();

	Date getLastModificationDate();

	void setLastModificationDate(Date date);
}