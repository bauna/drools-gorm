package org.drools.gorm.marshalling

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.HashMap
import java.util.IdentityHashMap
import java.util.Map

import org.drools.marshalling.ObjectMarshallingStrategy
import org.drools.marshalling.ObjectMarshallingStrategyAcceptor
import org.drools.gorm.GrailsIntegration
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

public class GrailsPlaceholderResolverStrategy
    implements
    ObjectMarshallingStrategy {
    
	private GrailsApplication grailsApplication

    public GrailsPlaceholderResolverStrategy() {
        this.grailsApplication = GrailsIntegration.getGrailsApplication()
    }  

    public Object read(ObjectInputStream os) throws IOException,
                                                       ClassNotFoundException {
    	String domain_name = os.readObject()
    	Object domain = this.grailsApplication.getClassForName(domain_name)
    	Long id = os.readLong()
        return domain.get(id)
    }

    public void write(ObjectOutputStream os,
                      Object object) throws IOException {
        os.writeObject( object.class.name )
        os.writeLong((Long) object.id)
    }

    public boolean accept(Object object) {
        return this.grailsApplication.isArtefactOfType(
        						DomainClassArtefactHandler.TYPE, object.class)
    }
}
