
package aisgateway;

import dk.dma.ais.reader.*;
import dk.dma.ais.message.*;

import edu.nps.moves.dis.*;
import edu.nps.moves.disutil.*;
import edu.nps.moves.spatial.*;

import java.util.function.*;
import java.util.*;
import java.util.concurrent.*;


/**
 * From https://github.com/dma-ais/AisLib
 * 
 * This connects to the NPS AIS server over TCP port 9010 and receives
 * AIS messages. Based on the messages we can gateway this to DIS, HLA
 * RPR-FOM, or whatever.
 * 
 * @author DMcG
 */
public class AisGateway {

   
    public static void main(String[] args) 
    {
        try
        {
            // Connect to the NPS server
            AisReader reader = AisReaders.createReader("172.20.70.143", 9010);
            
            // This hashmap has a key of the ship MMSI number, and 
            // a value of "ShipInfo", which contains information about one ship
            // as reported by AIS. It's concurrent, so multiple threads can access
            //  it at once.
            ConcurrentHashMap<Integer, ShipInfo> database = new ConcurrentHashMap<Integer, ShipInfo>();
            
            // Starts another execution thread that runs concurrently with this.
            // The Heartbeat run() method will start executing at the same time,
            // while execution will continue after line this at the same time.
            Heartbeat hb = new Heartbeat(database);
            Thread hbThread = new Thread(hb);
            hbThread.start();
            
            // Starts a thread that removes ships from the database if they have
            // sent no AIS message within a time period
            AisTimeout timeout = new AisTimeout(database);
            Thread timeoutThread = new Thread(timeout);
            timeoutThread.start();
            
            // Uses some fancy lambda stuff from Java 8. 
            reader.registerHandler(new Consumer<AisMessage>() 
            {
                // We received an AIS messge
                
                @Override
                public void accept(AisMessage aisMessage) 
                {
                    //System.out.println("message id: " + aisMessage.getMsgId());

                    // There are several types of messages. For now just trap the
                    // position reports.
                    
                    switch(aisMessage.getMsgId())
                    {
                        // Message types 1, 2 and 3 are all variants of position reports
                        // See www.navcen.uscg.gov/?pageName=AISMessages
                        // Types 1, 2, and 3 are all position reports
                        case 1:
                        case 2:
                        case 3:
                            //System.out.println("Position report");
                            
                            // Cast it to a position report message type
                            AisPositionMessage positionMessage = (AisPositionMessage)aisMessage;

                            // Extract relevant data
                            
                            // Knots, 1/10 knot steps (0-1002.2 knots)
                            int speedOverGround = positionMessage.getSog();

                            // Lat/lon
                            AisPosition location = positionMessage.getPos();
                            double lat = location.getLatitudeDouble();
                            double lon = location.getLongitudeDouble();

                            // MMSI number
                            int userId = aisMessage.getUserId();
                            
                            // In degrees; 511=not available/default
                            int heading = positionMessage.getTrueHeading();

                            /*
                            System.out.println("ID: " + userId + 
                                    " Location: " + location + 
                                    " Speed, tenths of knots:" + speedOverGround +
                                    " true heading:" + heading +
                                    " lat: " + lat +
                                    " lon:" + lon);
                                    */
                            
                            // Do geographic filtering here
                            if( !(lat > 34 && lat < 32) && !(lon > -122 && lon < -120) )
                                break;
                            
                            // Look up the ship, based on the MMSI number
                            ShipInfo shipInfo = database.get(userId);
                            
                            // If we get null back, that means we haven't heard from this ship
                            // before. Add it to the database.
                            if(shipInfo == null)
                            {
                                // OK, we've got data; how to send this in DIS?
                                EntityStatePdu espdu = new EntityStatePdu();
                                
                                ShipInfo newShipInfo = new ShipInfo(espdu);
                                
                               // Add entity type info, entity ID info to PDU,
                               // other stuff that doesn't change here. Should also
                                // add dead reckoning algorithm, and the ship name
                                // to the marking field.
                                espdu.getEntityType().setEntityKind((short) 1);  // Entity
                                espdu.getEntityType().setDomain((short)3);       // surface
                                espdu.getEntityType().setCountry(225);           // US; not strictly true
                                espdu.getEntityType().setCategory((short)61);    // non-combatant ship
                                espdu.getEntityType().setSubcategory((short)5);  // small fishing boat
                                
                                
                               // Set the entity ID
                               EntityID id = EntityIDs.getInstance().nextID();
                               espdu.setEntityID(id);
                                
                               database.put(userId, newShipInfo);
                               
                               shipInfo = newShipInfo;
                            }
                            
                            // Convert it to a DIS cartesian coordindate system
                            double[] disPosition = CoordinateConversions.getXYZfromLatLonDegrees(lat, lon, 0.0);

                            shipInfo.espdu.getEntityLocation().setX(disPosition[0]);
                            shipInfo.espdu.getEntityLocation().setY(disPosition[1]);
                            shipInfo.espdu.getEntityLocation().setZ(disPosition[2]);
                            
                            shipInfo.lastAISUpdate = new Date();
                            
                            // Send espdu to network here
                            Network.getInstance().sendPdu(shipInfo.espdu);

                            break;

                        // Static position report, sometimes used for navigation aides
                        case 5:
                            //System.out.println("Static position report");
                            // Get the ship name here and insert that into
                            // the marking field of the PDU. The static reports
                            // come in much less frequently than position reports,
                            // so it's possible we have no entry for the static 
                            // report. Also, this means we may have no ship name
                            // in the database fora  while.
                            
                            AisStaticCommon staticMessage = (AisStaticCommon)aisMessage;
                            int mmsi = aisMessage.getUserId();
                            //System.out.println("vessel name: " + staticMessage.getName());
                            ShipInfo info = database.get(mmsi);
                            if(info != null)
                            {
                                info.espdu.getMarking().setCharacters(staticMessage.getName());
                            }
                               
                            break;

                        default:
                            //System.out.println("other type of message");
                    }
                }
        });
        
        // Start reading from the AIS feed
        reader.start();
        reader.join();
        
        }
        catch(Exception e)
        {
            System.out.println(e);
            e.printStackTrace();
        }
    }
    
}