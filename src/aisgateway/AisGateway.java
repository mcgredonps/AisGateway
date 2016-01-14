
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
            ConcurrentHashMap<Integer, ShipInfo> database = new ConcurrentHashMap<Integer, ShipInfo>();
            
            // Starts another execution thread that runs concurrently with this.
            // The Heartbeat run() method will start executing at the same time,
            // and execution will continue after this at the same time.
            Heartbeat hb = new Heartbeat(database);
            Thread hbThread = new Thread(hb);
            hbThread.start();
            
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
                            
                            //if( !(lat > 34 && lat < 32) && !(lon > -122 && lon < -120) )
                            //    break;
                            
                            ShipInfo shipInfo = database.get(userId);
                            
                            if(shipInfo == null)
                            {
                                // OK, we've got data; how to send this in DIS?
                                EntityStatePdu espdu = new EntityStatePdu();
                                
                                ShipInfo newShipInfo = new ShipInfo(espdu);
                                
                               // Add entity type info, entity ID info to PDU,
                               // other stuff that doesn't change
                               database.put(userId, newShipInfo);
                               
                               shipInfo = newShipInfo;
                            }
                            
                            // Convert it to a DIS cartesian coordindate system
                            double[] disPosition = CoordinateConversions.getXYZfromLatLonDegrees(lat, lon, 0.0);

                            shipInfo.espdu.getEntityLocation().setX(disPosition[0]);
                            shipInfo.espdu.getEntityLocation().setY(disPosition[1]);
                            shipInfo.espdu.getEntityLocation().setZ(disPosition[2]);

                            break;

                        // Static position report, sometimes used for navigation aides
                        case 5:
                            //System.out.println("Static position report");
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