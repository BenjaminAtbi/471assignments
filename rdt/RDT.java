
/**
 * @author Benjamin Atbi
 *
 */
package rdt;

import java.io.*;
import java.net.*;
import java.sql.Array;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.*;

public class RDT {

	public static final int MSS = 10; // Max segement size in bytes
	public static final int RTO = 500; // Retransmission Timeout in msec
	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;  
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat
	public static int protocol;
	
	public static double lossRate = 0.0;
	public static Random random = new Random(); 
	public static Timer timer = new Timer();	
	
	private DatagramSocket socket; 
	private InetAddress dst_ip;
	private int dst_port;
	private int local_port; 
	
	private RDTBuffer sndBuf;
	private RDTBuffer rcvBuf;
	
	private ReceiverThread rcvThread;


	RDT (String dst_hostname_, int dst_port_, int local_port_, int protocol_)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		protocol = protocol_;
		try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(MAX_BUF_SIZE);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else 
			rcvBuf = new RDTBuffer(MAX_BUF_SIZE);
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port, protocol);
		rcvThread.start();
	}

	RDT (String dst_hostname_, int dst_port_, int local_port_, int sndBufSize, int rcvBufSize, int protocol_)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		protocol = protocol_;
		 try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(sndBufSize);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else 
			rcvBuf = new RDTBuffer(rcvBufSize);
		
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port, protocol);
		rcvThread.start();
	}
	
	public static void setLossRate(double rate) {lossRate = rate;}
	
	// called by app
	// returns total number of sent bytes  
	public int send(byte[] data, int size) {
		if (size < MSS){
		sendDataSegment(data);
		} else {
			System.out.println("splitting data segments");
			for(int i = 0;  i*MSS < size; i++){
				sendDataSegment(Arrays.copyOfRange(data, i*MSS, Math.min((i+1)*MSS, size)));
			}
		}
			
		return size;
	}

	private void sendDataSegment(byte[] data){
		if(protocol == GBN){
			sendDataSegmentGBN(data);
		} else {
			sendDataSegmentSR(data);
		}
	}

	private void sendDataSegmentGBN(byte[] data){
//		System.out.println("creating segment with data:"+Arrays.toString(data));
		RDTSegment seg = RDTSegment.createDataSegment(data,sndBuf.next);
		sndBuf.putNext(seg);
		if(seg.seqNum == sndBuf.base){
			System.out.println("SEND: setting timeout for "+seg.seqNum);
			seg.setTimeoutHandler(sndBuf, socket, dst_ip, dst_port);
		}
		System.out.printf("SEND: Sending segment SEQ:%d, LEN:%d\n",seg.seqNum, seg.length);
		Utility.udp_send(seg, socket, dst_ip, dst_port);
	}

	private void sendDataSegmentSR(byte[] data){
//		System.out.println("creating segment with data:"+Arrays.toString(data));
		RDTSegment seg = RDTSegment.createDataSegment(data,sndBuf.next);
		sndBuf.putNext(seg);
		System.out.println("SEND: setting timeout for "+seg.seqNum);
		seg.setTimeoutHandler(sndBuf, socket, dst_ip, dst_port);
		System.out.printf("SEND: Sending segment SEQ:%d, LEN:%d\n",seg.seqNum, seg.length);
		Utility.udp_send(seg, socket, dst_ip, dst_port);
	}
	
	// called by app
	// receive one segment at a time
	// returns number of bytes copied in buf
	public int receive (byte[] buf, int size)
	{
		RDTSegment seg = rcvBuf.getBase();
		seg.getData(buf, size);
		return seg.length;
	}

	// called by app
	public void close() {
		// OPTIONAL: close the connection gracefully
		// you can use TCP-style connection termination process
	}
	
}  // end RDT class

