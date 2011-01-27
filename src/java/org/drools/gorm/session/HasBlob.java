package org.drools.gorm.session;


public interface HasBlob<T extends Number> {
    String GORM_UPDATE_SET = "__gorm_update_set";
    
    T getId();
    byte[] generateBlob();
    String getTableName();
    boolean isDeleted();
}
