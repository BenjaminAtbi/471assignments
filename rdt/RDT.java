
/**
 * @author mohamed
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
	public static int protocol = GBN;
	
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
		// schedule timeout for segment(s) 
			
		return size;
	}

	private void sendDataSegment(byte[] data){
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

class ReceiverThread extends Thread {
	RDTBuffer rcvBuf, sndBuf;
	DatagramSocket socket;
	InetAddress dst_ip;
	int dst_port;
	int payloadSize;
	int protocol;
	
	ReceiverThread (RDTBuffer rcv_buf, RDTBuffer snd_buf, DatagramSocket s, 
			InetAddress dst_ip_, int dst_port_, int protocol_) {
		rcvBuf = rcv_buf;
		sndBuf = snd_buf;
		socket = s;
		dst_ip = dst_ip_;
		dst_port = dst_port_;
		payloadSize = RDT.MSS + RDTSegment.HDR_SIZE;
		protocol = protocol_;
	}	

	public void run() {
		
		socket.connect(dst_ip, dst_port);

		while(true) {

			byte[] payload = new byte[payloadSize];
			DatagramPacket pkt = new DatagramPacket(payload,payloadSize);
			try {
				socket.receive(pkt);
			} catch (IOException e) {
				System.out.println("ReceiverThread error during receive:"+ e);
			}

			RDTSegment seg = new RDTSegment();
			makeSegment(seg, payload);
			System.out.println("ReceiverThread received segment: "+seg);
			//checksum

			if(seg.containsAck()){
				Ack(seg);
			}

			if(seg.containsData()){
				Receive(seg);
			}
				
		}
	}

	private void Receive(RDTSegment seg){
		switch(RDT.protocol){
			case RDT.GBN:
				GBNReceive(seg);
				break;
			case RDT.SR:
				
				break;
			default:
				System.out.println("Error in ReceiverThread Receive: unknown protocol");
		}
	}

	private void GBNReceive(RDTSegment seg){
		
		System.out.printf("GBN Receive: seqNum %d expected %d\n", seg.seqNum, rcvBuf.next);

		//receive buffer next index is the expected sequence number, start at 0
		RDTSegment ack;
		if(seg.seqNum == rcvBuf.next){
			rcvBuf.putNext(seg);
			ack = RDTSegment.createAckSegment(seg.seqNum);
			System.out.printf("GBN Receive: Received, sending Ack %d\n", ack.ackNum);
		//else send last correct ACK (1 before expected)
		} else {
			ack = RDTSegment.createAckSegment(rcvBuf.next - 1);
			System.out.printf("GBN Receive: Discarded, sending Ack %d\n", ack.ackNum);
		}
		Utility.udp_send(ack, socket, dst_ip, dst_port);

	}

	private void Ack(RDTSegment seg){
		System.out.println("Processing ACK");
		switch(RDT.protocol){
			case RDT.GBN:
				GBNAck(seg);
				break;
			case RDT.SR:
				
				break;
			default:
				System.out.println("Error in ReceiverThread ACK: unknown protocol");
		}
	}

	private void GBNAck(RDTSegment seg){

		//for each segment in send window, if sequence number is <= ACK, release it
		while(sndBuf.base <= seg.ackNum){
			System.out.printf("GBN ACK: ackNum %d base %d\n", seg.ackNum, sndBuf.base);
			RDTSegment old_seg = sndBuf.getBase();
			if(old_seg != null && old_seg.timeoutHandler != null){
				old_seg.timeoutHandler.cancel();
			}
		}

		//reset timer for new base
		RDTSegment new_base = sndBuf.accessBase();
		if(new_base != null){
			new_base.setTimeoutHandler(sndBuf, socket, dst_ip, dst_port);
		}

	}
	
//	 create a segment from received bytes 
	void makeSegment(RDTSegment seg, byte[] payload) {
	
		seg.seqNum = Utility.byteToInt(payload, RDTSegment.SEQ_NUM_OFFSET);
		seg.ackNum = Utility.byteToInt(payload, RDTSegment.ACK_NUM_OFFSET);
		seg.flags  = Utility.byteToInt(payload, RDTSegment.FLAGS_OFFSET);
		seg.checksum = Utility.byteToInt(payload, RDTSegment.CHECKSUM_OFFSET);
		seg.rcvWin = Utility.byteToInt(payload, RDTSegment.RCV_WIN_OFFSET);
		seg.length = Utility.byteToInt(payload, RDTSegment.LENGTH_OFFSET);

		//Note: Unlike C/C++, Java does not support explicit use of pointers! 
		// we have to make another copy of the data
		// This is not effecient in protocol implementation
		for (int i=0; i< seg.length; i++)
			seg.data[i] = payload[i + RDTSegment.HDR_SIZE];
	}
	
} // end ReceiverThread class

