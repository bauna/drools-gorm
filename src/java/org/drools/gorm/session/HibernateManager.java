package org.drools.gorm.session;

import org.hibernate.Session;

public interface HibernateManager {
    Session getApplicationScopedEntityManager();
    
    Session getCommandScopedEntityManager();
    
    void beginCommandScopedEntityManager();
    
    void endCommandScopedEntityManager();

    void dispose();
}
