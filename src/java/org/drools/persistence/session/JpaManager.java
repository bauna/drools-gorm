package org.drools.persistence.session;

import org.hibernate.Session;

public interface JpaManager {
    Session getApplicationScopedEntityManager();
    
    Session getCommandScopedEntityManager();
    
    void beginCommandScopedEntityManager();
    
    void endCommandScopedEntityManager();

    void dispose();
}
