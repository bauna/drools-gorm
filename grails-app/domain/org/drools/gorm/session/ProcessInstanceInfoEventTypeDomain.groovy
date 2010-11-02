package org.drools.gorm.session


class ProcessInstanceInfoEventTypeDomain {

	static belongsTo = [processInstanceInfo:ProcessInstanceInfoDomain]
	String name
	
    // map name because name to long for ORACLE
    static mapping = {
        table 'proc_inst_info_ev_type_domain'
    }
}
