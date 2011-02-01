package org.drools.gorm.session


class ProcessInstanceEventInfoDomain implements ProcessInstanceEventInfo {

    long id
    String eventType
    long processInstanceId
    
    // map name because name to long for ORACLE
    static mapping = {
        table 'proc_inst_event_info'
    }
}
