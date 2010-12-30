package org.drools.gorm.session;

public interface HasBlob {
    String GORM_UPDATE_SET = "__gorm_update_set";
    
    void generateBlob();
}
