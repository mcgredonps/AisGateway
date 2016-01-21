/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package aisgateway;

import java.util.*;
import java.util.concurrent.*;

import edu.nps.moves.dis.*;

/**
 * This object loops through the database of entities every five seconds,
 * sending an ESPDU for each ship entry
 * 
 * @author DMcG
 */
public class Heartbeat implements Runnable
{
    public static final int HEARTBEAT_PERIOD = 5000;
    
    ConcurrentHashMap<Integer, ShipInfo> database;
    
    public Heartbeat(ConcurrentHashMap<Integer, ShipInfo> database)
    {
       this.database = database; 
    }
    
    
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

          System.out.println();
          //System.out.println(" Database contents MMSI entries:");
          System.out.println("    Number of ships: " + database.size());
          //System.out.println("====");
          
          // Loop through all the MMSIs in the database, and retrieve the corresponding
          // ship information for eash MMSI
          while(it.hasNext())
          {
              Integer mmsi = (Integer)it.next();
              //System.out.println("MMSI: " + mmsi);
              ShipInfo shipInfo = database.get(mmsi);

              EntityStatePdu espdu = shipInfo.espdu;
             
              Network.getInstance().sendPdu(espdu);
              System.out.println("Sent update for ship name " + new String(espdu.getMarking().getCharacters()));
              shipInfo.lastDISUpdate = new Date();
             
          }
          //System.out.println("====");
          //System.out.println("   Number of ships in database: " + database.size());

        }
        catch(Exception e)
        {
            System.out.println(e + " Concurrent access problem, probably");
        }

        // Go to sleep for five seconds, then do it again
        try
        {
          Thread.sleep(HEARTBEAT_PERIOD);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
      }

}

}
