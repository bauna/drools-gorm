package org.drools.gorm

import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.drools.runtime.process.WorkItemHandler
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.apache.commons.lang.WordUtils
import org.drools.gorm.session.SessionInfoDomain


public class GrailsIntegration {
    
	static GrailsApplication getGrailsApplication() {
            return new SessionInfoDomain().domainClass.grailsApplication
	}
	
	static ApplicationContext getMainContext() {
		return getGrailsApplication().mainContext
	}
	
	static GormDomainService getGormDomainService() {
		def ctx = getMainContext()
        return ctx.gormDomainService 
	}

	static PlatformTransactionManager getTransactionManager() {
		def ctx = getMainContext()
		return ctx.transactionManager
	}

	static ClassLoader getGrailsClassLoader() {
		def ctx = getMainContext()
		return ctx.classLoader
	}

    static SessionFactory getCurrentSessionFactory() {
        def ctx = getMainContext()
        return ctx.sessionFactory
    }
    
	static Session getCurrentSession() {
		def ctx = getMainContext()
		return ctx.sessionFactory.currentSession
	}
	
	static void flushCurrentSession() {
		getCurrentSession().flush()
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
    		def ctx = getMainContext()
    		return ctx.getBeansOfType(workItemHandlerClass.clazz).values().toArray()[0]
    	}
	}
}
