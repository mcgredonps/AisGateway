
package aisgateway;

import edu.nps.moves.dis.*;

/**
 * Hands out EntityIDs. A singleton class. 
 * 
 * @author DMcG
 */
public class EntityIDs 
{
    public static EntityIDs sharedInstance = null;
    
    public short site = 23;
    private short application = 42;
    
    private int nextID = 1;
    
    public synchronized static EntityIDs getInstance()
    {
        if(sharedInstance == null)
        {
            sharedInstance = new EntityIDs();
        }
        
        return sharedInstance;
        
    }
    
    private EntityIDs()
    {
        
    }
    
    /**
     * We can worry about entityID rollover here if we like.
     * @return 
     */
    public synchronized EntityID nextID()
    {
        EntityID id = new EntityID();
        id.setApplication(application);
        id.setSite(site);
        id.setEntity((short)nextID);
        nextID++;
        
        return id;
    }

}
