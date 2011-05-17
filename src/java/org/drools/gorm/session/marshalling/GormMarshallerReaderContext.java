package org.drools.gorm.session.marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.util.Map;

import org.drools.common.BaseNode;
import org.drools.common.InternalRuleBase;
import org.drools.marshalling.impl.MarshallerReaderContext;
import org.drools.marshalling.impl.ObjectMarshallingStrategyStore;
import org.drools.runtime.Environment;

public class GormMarshallerReaderContext extends MarshallerReaderContext {

    private ClassLoader userClassLoader = null;
    
    public GormMarshallerReaderContext(InputStream stream, InternalRuleBase ruleBase,
            Map<Integer, BaseNode> sinks, ObjectMarshallingStrategyStore resolverStrategyFactory,
            boolean marshalProcessInstances, boolean marshalWorkItems, Environment env)
            throws IOException {
        super(stream, ruleBase, sinks, resolverStrategyFactory, marshalProcessInstances, marshalWorkItems,
                env);
    }

    public GormMarshallerReaderContext(InputStream stream, InternalRuleBase ruleBase,
            Map<Integer, BaseNode> sinks, ObjectMarshallingStrategyStore resolverStrategyFactory,
            Environment env) throws IOException {
        super(stream, ruleBase, sinks, resolverStrategyFactory, env);
    }

    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    public void setUserClassLoader(ClassLoader userClassLoader) {
        this.userClassLoader = userClassLoader;
    }

    @Override
    protected Class< ? > resolveClass(ObjectStreamClass desc) throws IOException,
                                                             ClassNotFoundException {
        try {
            return super.resolveClass(desc);
        } catch (ClassNotFoundException e) {
            if (getUserClassLoader() != null) {
                return getUserClassLoader().loadClass(desc.getName());
            }
            throw e;
        }
    }
}
