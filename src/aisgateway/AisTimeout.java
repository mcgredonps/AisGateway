
package aisgateway;

import java.util.Iterator;
import java.util.*;
import java.util.concurrent.*;

/**
 * Causes a ship to be removed from the database if it hasn't sent an
 * AIS message in some period of time
 * 
 * @author DMcG
 */
public class AisTimeout implements Runnable
{
    /** Timeout, 1000 ms times 60 sec/min times min */
    //public static final int AIS_MESSAGE_TIMEOUT = 1000 * 60 * 15;
    public static final int AIS_MESSAGE_TIMEOUT = 500;

    private ConcurrentHashMap<Integer, ShipInfo> database = null;
    
    public AisTimeout(ConcurrentHashMap<Integer, ShipInfo> database)
    {
       this.database = database; 
    }
    
    /**
     * Removes entities from the database if we haven't received an AIS message
     * within the timeout period
     */
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                // Gets a list of all the AIS MMSIs in the database
                Set keys = database.keySet();

                // Iterator lets us retrieve the MMSIs, one by one
                Iterator it = keys.iterator();
                
                while(it.hasNext())
                {
                    Integer id = (Integer)it.next();
                    ShipInfo shipInfo = database.get(id);
                    Date now = new Date();
                    
                    if(shipInfo.lastAISUpdate.getTime() + AIS_MESSAGE_TIMEOUT > now.getTime())
                    {
                        database.remove(id);
                        System.out.println("Removed ship from database: " + id);
                    }
                }
                
                Thread.sleep(1000 * 60 * 1);
                
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
        }
        
    }
}
