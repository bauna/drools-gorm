package org.drools.gorm

import org.springframework.transaction.PlatformTransactionManager
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.drools.runtime.process.WorkItemHandler
import org.apache.commons.lang.WordUtils

public class GrailsIntegration {

	static GORMDomainService getGORMDomainService() {
		def ctx = ApplicationHolder.application.mainContext
		return ctx.GORMDomainService
	}

	static PlatformTransactionManager getTransactionManager() {
		def ctx = ApplicationHolder.application.mainContext
		return ctx.transactionManager
	}

	static GrailsApplication getGrailsApplication() {
		return ApplicationHolder.application
	}

	static ClassLoader getGrailsClassLoader() {
		def ctx = ApplicationHolder.application.mainContext
		return ctx.classLoader
	}

	static void flushCurrentSession() {
		def ctx = ApplicationHolder.application.mainContext
		ctx.sessionFactory.currentSession.flush()
	}
	
	static WorkItemHandler getWorkItemHandlerServices(workItemName) {
		def workItemHandlerClasses = getGrailsApplication().serviceClasses.findAll {
    		it.clazz.interfaces.any {it == WorkItemHandler}
    	}

    	def workItemHandlerClass = workItemHandlerClasses.find {
			def clazz = it.clazz
    		// try to get configured work item name from class property
	        def configuredName = GrailsClassUtils.getStaticPropertyValue(clazz, 'workItemName')
	        if (configuredName == workItemName) {
	        	return true
	        } else {
	        	// check if class name matches work item name
	    		def nameWithoutSpaces = workItemName.replaceAll(' ','')
	    		def handlerName = WordUtils.uncapitalize(nameWithoutSpaces) + 'WorkItemService'
	    		return (clazz.simpleName == handlerName)
	        }
    	}
    	
    	if (workItemHandlerClass != null) {
    		def ctx = ApplicationHolder.application.mainContext
    		return ctx.getBeansOfType(workItemHandlerClass.clazz).values().toArray()[0]
    	}
	}
}
