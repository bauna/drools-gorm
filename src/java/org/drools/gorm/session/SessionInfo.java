package org.drools.gorm.session;

import java.util.Date;

import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper;

public interface SessionInfo {
    
    String SAFE_GORM_COMMIT_STATE = "__gormCanCommit";

	int getId();

	void setMarshallingHelper(GormSessionMarshallingHelper helper);

	byte[] getData();
	
	Date getStartDate();

	Date getLastModificationDate();

	void setLastModificationDate(Date date);
}
