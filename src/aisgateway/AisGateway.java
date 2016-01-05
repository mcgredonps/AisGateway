/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aisgateway;

import dk.dma.ais.reader.*;
import dk.dma.ais.message.*;

import edu.nps.moves.dis.*;

import java.util.function.*;


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
            
            // Uses some fancy lambda stuff from Java 8. 
            reader.registerHandler(new Consumer<AisMessage>() 
            {
                @Override
                public void accept(AisMessage aisMessage) 
                {
                    //System.out.println("message id: " + aisMessage.getMsgId());

                    // There are several types of messages. For now just trap the
                    // position reports.
                    
                    switch(aisMessage.getMsgId())
                    {
                        // Message types 1, 2 and 3 are all variants of position reports
                        case 1:
                        case 2:
                        case 3:
                            System.out.println("Position report");
                            
                            // Cast it to a position report message type
                            AisPositionMessage positionMessage = (AisPositionMessage)aisMessage;

                            // Extract relevant data
                            
                            // Knots, 1/10 knot steps (0-1002.2 knots)
                            int speedOverGround = positionMessage.getSog();

                            // Lat/lon
                            AisPosition location = positionMessage.getPos();

                            // MMSI number
                            int userId = aisMessage.getUserId();
                            
                            // In degrees; 511=not available/default
                            int heading = positionMessage.getTrueHeading();

                            System.out.println("ID: " + userId + 
                                    " Location: " + location + 
                                    " Speed, tenths of knots:" + speedOverGround +
                                    " true heading:" + heading);

                            // OK, we've got data; how to send this in DIS?
                            EntityStatePdu espdu = new EntityStatePdu();
                            
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