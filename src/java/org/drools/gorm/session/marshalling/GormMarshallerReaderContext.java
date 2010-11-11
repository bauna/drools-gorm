/**
 * 
 */
package org.drools.gorm.session.marshalling;

import groovy.lang.GroovyObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.util.Map;

import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.drools.common.BaseNode;
import org.drools.common.InternalRuleBase;
import org.drools.gorm.GrailsIntegration;
import org.drools.marshalling.impl.MarshallerReaderContext;
import org.drools.marshalling.impl.ObjectMarshallingStrategyStore;
import org.drools.runtime.Environment;

public class GormMarshallerReaderContext extends MarshallerReaderContext {
	private ClassLoader grailsClassLoader;

	public GormMarshallerReaderContext(InputStream stream,
            InternalRuleBase ruleBase,
            Map<Integer, BaseNode> sinks,
            ObjectMarshallingStrategyStore resolverStrategyFactory,
            Environment env) throws IOException {
		
		super( stream, ruleBase, sinks, resolverStrategyFactory, env);

	    this.grailsClassLoader = GrailsIntegration.getGrailsClassLoader();
	    this.enableResolveObject(true);
	};
	
	public GormMarshallerReaderContext(InputStream stream,
	            InternalRuleBase ruleBase,
	            Map<Integer, BaseNode> sinks,
	            ObjectMarshallingStrategyStore resolverStrategyFactory,
	            boolean marshalProcessInstances,
	            boolean marshalWorkItems,
	            Environment env) throws IOException {
		
		super(stream,ruleBase,sinks, resolverStrategyFactory, 
				marshalProcessInstances, marshalWorkItems, env);

	    this.grailsClassLoader = GrailsIntegration.getGrailsClassLoader();
	    this.enableResolveObject(true);
	};

	protected Class<?> resolveClass(ObjectStreamClass desc)
								throws IOException, ClassNotFoundException
	{
		String name = desc.getName();
		try {
			return Class.forName(name, false, this.grailsClassLoader);
		} catch (ClassNotFoundException ex) {
			return super.resolveClass(desc);
		}
	}
	
	protected Object resolveObject(Object obj) throws IOException {
		Class<?> clazz = obj.getClass();
		if (clazz != null) {
			if (GrailsIntegration.getGrailsApplication().isArtefactOfType(
					DomainClassArtefactHandler.TYPE, clazz)) {
				String className = clazz.getName();
				Long objId = (Long) ((GroovyObject) obj).invokeMethod("getId", null);
				Class<?> domainClass = ApplicationHolder.getApplication().getClassForName(className);

				obj = InvokerHelper.invokeMethod(domainClass, "get", objId);				
			}
		}
		return obj;
	};
}