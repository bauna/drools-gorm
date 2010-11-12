package org.drools.gorm.test

class DroolsTest implements Serializable {

	String name
	Long value
	Date dateCreated
	Date lastUpdated
	
    static constraints = {
		name(maxSize:64, unique:true)
		value(blank:true)
		dateCreated(display:false)
		lastUpdated(display:false)
	}
}
