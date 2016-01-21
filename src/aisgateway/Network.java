
package aisgateway;

import java.net.*;
import java.io.*;
import edu.nps.moves.dis.*;
import edu.nps.moves.disutil.*;

/**
 *
 * @author DMcG
 */
public class Network 
{
    public static final int PORT = 3000;
    public static final String DESTINATION_ADDRESS = "172.20.159.255";
    
    private static Network networkInstance = null;
    private InetAddress destinationAddress = null;
    
    private DatagramSocket socket = null;
    
    /**
     * Singleton pattern; retrieve the single, shared instance this way
     * @return 
     */
    public static synchronized Network getInstance()
    {
        if(networkInstance == null)
        {
            networkInstance = new Network(PORT);
        }
        
        return networkInstance;
    }
    
    
    private Network(int port)
    {
        try
        {
            socket = new DatagramSocket(3000);
            destinationAddress = InetAddress.getByName(DESTINATION_ADDRESS);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        
    }
    
    public synchronized void sendPdu(Pdu aPdu)
    {
        try
        {
            byte[] buffer = aPdu.marshalWithDisAbsoluteTimestamp();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destinationAddress, PORT);
            socket.send(packet);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

}
