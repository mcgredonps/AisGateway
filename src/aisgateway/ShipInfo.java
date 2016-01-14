
package aisgateway;

import edu.nps.moves.dis.*;
import java.util.*;

/**
 * Represents information about one ship in AIS, and its relation to DIS 
 * protocol information.
 * 
 * @author DMcG
 */
public class ShipInfo 
{
    /** The entity state pdu for this ship */
    EntityStatePdu espdu = null;
    
    /** The last time we received an update for this ship from AIS */
    Date lastAISUpdate;
    
    /** The last time we sent a DIS update */
    Date lastDISUpdate;
    
    // Any other information we like
    
    public ShipInfo(EntityStatePdu espdu)
    {
        this.espdu = espdu;
        lastAISUpdate = new Date(); // Now
        lastDISUpdate = new Date();
    }

}
