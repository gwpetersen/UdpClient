/**
 * Garett Petersen
 * 
 * 2/17/2016
 * 
 * Professor: Nick Pantic
 * 
 * Purpose: this assignment we turned our ipv4 packet and built udp on top of it. UDP packets
 * were sent in as data into the ipv4 packet. 
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import javax.xml.bind.DatatypeConverter;

public class UdpClient {
	private final static short HEADERSIZE = 20; //ipv4 header size is 20(bytes)
	private InputStream socketIn;
	private PrintStream out;
	private int udpPort = 0;
	private byte[] source = new byte[4];
	private byte[] destip = new byte[4];
	
	
	public static void main(String[] args) throws UnknownHostException, IOException { 
		UdpClient udp = new UdpClient();
		
		udp.run();
	}
	
	private void run() throws UnknownHostException, IOException { 
		String host = "cs380.codebank.xyz";
		int port = 38005;
		short counter = 1;
		short DATASIZE = 2;
		
		source[0] = 0b0;  //0
		source[1] = 0b0;  //0
		source[2] = 0b01; //1
		source[3] = 0b01; //1
		
		destip[0] = 0b00110100;
		destip[1] = 0b00100001;
		destip[2] = -0b1111101; //-125
		destip[3] = 0b00010000;

		
		
		try (Socket socket = new Socket(host, port)) { 
			socketIn = (socket.getInputStream());
			out = new PrintStream(socket.getOutputStream());
			System.out.println("Connected to " + host + ":" + port + "!\n");
			
			// used to send the server deadbeef handshake
			byte[] hand = {	(byte)0xDE,(byte)0xAD, (byte)0xBE, (byte)0xEF }; //0xDEADBEEF, handshake message
			// pass the deadbeef to a method called hanshake
			udpPort = handshake(hand);
			System.out.println("Handshake completed! UDP Port:" + udpPort + "\n");
			
			long totalTime = 0;
			while(counter < 13) { //send 12 packets
				byte[] randomData = new byte[DATASIZE];
					new Random().nextBytes(randomData);
				byte[] udp = buildUDP(randomData);
				byte[] packet = buildPacket(udp);
				
				//printPacket(packet);
				long start = System.currentTimeMillis() % 1000;
				
				out.write(packet);
				byte[] b = new byte[4];
				int count = socketIn.read(b);
				
				long stop = System.currentTimeMillis() % 1000;
				
				System.out.println(counter + ") " + DatatypeConverter.printHexBinary(b) + " | " + (stop-start) + "(ms)");

				totalTime += (stop-start);
				DATASIZE *= 2;
				counter++;
			}
			System.out.println("\nAll packets sent... Average time:" + (totalTime/12) + "(ms)" + "\nClosing connection");
			socketIn.close();
			out.close();
		} //end try
		System.exit(0);
		
	}
	
	private byte[] buildPacket(byte[] data) { 
		byte packet[] = new byte[HEADERSIZE + data.length]; //HEADERSIZE is 20
		
		packet[0] = 0x4; // version
		packet[0] <<= 4;
		packet[0] |= 0x5; //length of header
		
		packet[1] = 0;
		
		short tempshort = (short) (HEADERSIZE + data.length);
		packet[2] = (byte) ((tempshort >>> 8) & 0xFF);
		packet[3] = (byte) (tempshort & 0xFF); //adjust based on data size
		
		packet[4] = 0;
		packet[5] = 0;
		
		packet[6] = 0b010; //flags
		packet[6] <<= 5;
		
		packet[6] |= 0b0;
		packet[7] = 0b0;
		
		packet[8] = 0b00110010; //ttl
		
		packet[9] = 0x11; //protocol for udp(17) not tcp
		
		for(int i=12, j=0; i<16; ++i, ++j) 
			packet[i] = source[j];
		
		for(int i=16, j=0; i<20; ++i, ++j) 
			packet[i] = destip[j];
		
		short[] shortarray = byteToShort(packet);
		short[] t = new short[1];
		t[0] = (short) checksum(shortarray);
		byte t1[] = toByteArray(t);

		packet[10] = t1[0]; //checksum locations
		packet[11] = t1[1]; 
		
		for(int i=20, j=0; j<data.length; ++i, ++j)
			packet[i] = data[j];
		
		return packet;
	}
	
	private byte[] buildUDP(byte[] data) { 
		byte[] udp = new byte[8 + data.length];
		
		udp[0] = 0; //source PORT
		udp[1] = 0;
		
		udp[6] = 0; //initialize checksum to 0
		udp[7] = 0;
		
		short tempshort = (short) udpPort;
			udp[2] = (byte) ((tempshort >>> 8) & 0xFF); //destination port
			udp[3] = (byte) (tempshort & 0xFF);
		
		tempshort = (short) (8 + data.length);
			udp[4] = (byte) ((tempshort >>> 8) & 0xFF); //length
			udp[5] = (byte) (tempshort & 0xFF);
		
		for(int i=8, j=0; j < data.length; ++i, ++j) //data potion of udp packet
			udp[i] = data[j]; //data
		
		short shortarray[] = byteToShort(udp);
		short checksum = (short)udpChecksum(shortarray, tempshort);
		
		udp[6] = (byte) ((checksum >>> 8) & 0xFF);
		udp[7] = (byte) (checksum & 0xFF);
		
		return udp;
	}
	
	/**
	 * udp checksum
	 */
	
	private long udpChecksum(short[] buf, short length) { 
		long sum = 0;
		short udp[] = new short[buf.length + 6];
		
		udp[0] = source[0];
		udp[0] <<= 8;
		udp[0] |= source[1];
		udp[1] = source[2];
		udp[1] <<= 8;
		udp[1] |= source[3];
		
		udp[2] = destip[0];
		udp[2] <<= 8;
		udp[2] |= destip[1];
		udp[3] = destip[2];
		udp[3] <<= 8;
		udp[3] |= destip[3];
		
		udp[4] = 0;
		udp[4] <<= 8;
		udp[4] |= 0x11; // udp protocol (17)
		
		udp[5] = length;
		
		for(int i=6, j=0; i<buf.length + 6; ++i, ++j) 
			udp[i] = buf[j];
		
		for(int i=0; i<udp.length; ++i) { 
			sum += (udp[i] & 0xFFFF);
			if((sum & 0xFFFF0000) > 0) { 
				sum &= 0xFFFF;
				sum++;
			}
		}
		sum = ~(sum & 0xFFFF);
		return sum;
	}
	
	private long checksum(short[] buf) { 
		long sum = 0;
		for(int i=0; i<10; ++i) { 
			sum += (buf[i] & 0xFFFF);
			if ((sum & 0xFFFF0000) > 0) { //carry 
				sum &= 0xFFFF;
				sum++;
			}
		}
		sum = ~(sum & 0xFFFF);
		return sum;
	}
	
	public static void printPacket(byte[] sendIt) {
		System.out.println("SENDING PACKET OF LENGTH " + sendIt.length);
		for (int i = 0; i < sendIt.length; i++) {
			String temp = "";
			for (int j = 7; j >= 0; j--)
				temp += ((0b1 << j & sendIt[i]) > 0) ? "1" : "0";
			if (i % 4 == 0) 
				System.out.println();
			while (temp.length() < 8) 
				temp += ("0" + temp);
			System.out.print(temp.substring(temp.length() - 8) + " ");
		}
		System.out.println();
	}
	
	/**
	 * First, we?ll do a ?handshaking? step where you send a single IPv4 packet with 4 bytes of data hard-coded
	 * to 0xDEADBEEF. Then, my server will respond with 2 bytes of raw data (not an IP packet, so you don?t
	 * need to be able to parse incoming IP packets) which you should treat as an unsigned 16-bit integer
	 * corresponding to a port number.
	 * @throws IOException 
	 */
	private short handshake(byte[] data) throws IOException { //data has deadbeef
		//sending deadbeef to ipv4 with a udp protocol...not the udp packet

		byte hand[] =buildPacket(data); 
		// whatever the packet responds with needs to be sent to the server. 
		out.write(hand);
		//used to read the cafebabe response of accepting my handshake
		byte[] b = new byte[4];
		// used to read the following two bytes of raw data
		byte[] twobytes = new byte[2];
		//this read is for the handshake itself..should be reading cafebabe
		int count = socketIn.read(b);
		System.out.println("Handshake response: " + DatatypeConverter.printHexBinary(b));
		//this reads the two byte of raw data...it is turned into a short and used as 
		// the port number and returned. 
		int twob =socketIn.read(twobytes);
		short[] portvalue = byteToShort(twobytes);
		
		return portvalue[0]; //return udp port
	}
	
	
	// same as the last project for ipv4
	
	private byte[] toByteArray(short[] message) {
		byte[] b = new byte[message.length * 2];
		for (int i = 0, j = 0; i < message.length; i++, j += 2) {
			b[j + 1] |= (message[i] & 0xFF);
			message[i] >>>= 8;
			b[j] |= (message[i] & 0xFF);
		}
		return b;
	}
	// same as ipv4 used in building packet
	private short[] byteToShort(byte[] message) {
		short[] shortMessage = new short[(message.length + 1) / 2];
		for (int i = 0, j = 0; j < message.length - 1; i++, j += 2) {
			shortMessage[i] |= (message[j] & 0xFF);
			shortMessage[i] <<= 8;
			shortMessage[i] |= (message[j + 1] & 0xFF);
		}
		return shortMessage;
	}
}