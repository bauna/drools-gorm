package org.drools.gorm.session;

import java.util.Date;

import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper;

public interface SessionInfo extends HasBlob {
    
	int getId();

	void setMarshallingHelper(GormSessionMarshallingHelper helper);

	byte[] getData();
	
	Date getStartDate();

	Date getLastModificationDate();

	void setLastModificationDate(Date date);
}
