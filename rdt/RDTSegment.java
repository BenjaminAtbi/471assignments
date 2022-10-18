/**
 * 
 * @author mohamed
 *
 */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

public class RDTSegment {
	public int seqNum;
	public int ackNum;
	public int flags;
	public int checksum; 
	public int rcvWin;
	public int length;  // number of data bytes (<= MSS)
	public byte[] data;

	public boolean ackReceived;
	
	public TimeoutHandler timeoutHandler;  // make it for every segment, 
	                                       // will be used in selective repeat
	
  // constants 
	public static final int SEQ_NUM_OFFSET = 0;
	public static final int ACK_NUM_OFFSET = 4;
	public static final int FLAGS_OFFSET = 8;
	public static final int CHECKSUM_OFFSET = 12;
	public static final int RCV_WIN_OFFSET = 16;
	public static final int LENGTH_OFFSET = 20;
	public static final int HDR_SIZE = 24; 
	public static final int FLAGS_ACK = 1;

	RDTSegment() {
		data = new byte[RDT.MSS];
		flags = 0; 
		checksum = 0;
		seqNum = 0;
		ackNum = 0;
		length = 0;
		rcvWin = 0;
		ackReceived = false;
	}

	public static RDTSegment createAckSegment(int ackNum){
		RDTSegment ack = new RDTSegment();
		ack.ackNum = ackNum;
		ack.flags = ack.flags | FLAGS_ACK;
		return ack;
	}

	public static RDTSegment createDataSegment(byte[] data, int seqNum){
		RDTSegment seg = new RDTSegment();
		seg.setData(data);
		seg.seqNum = seqNum;
		return seg;
	}
	
	public boolean containsAck() {
		return FLAGS_ACK == (flags & FLAGS_ACK) ;
	}
	
	public boolean containsData() {
		return length > 0;
	}

	public int computeChecksum() {
		// complete
		return 0;
	}
	public boolean isValid() {
		// complete
		return true;
	}

	public void setData(byte[] data_buf) {
		if(data_buf.length > RDT.MSS){
			throw new RuntimeException("Error in RDTSegment set Data: size too large");
		}

		for(int i = 0; i < data_buf.length; i++){
			 data[i] = data_buf[i];
		}
		length = data_buf.length;
	}

	//put segmnet data into buffer
	public void getData(byte[] data_buf, int size) {
		if(size < length){
			throw new RuntimeException("Error in RDTSegment get Data: buffer too small");
		}

		for (int i=0; i<length; i++){
			data_buf[i] = data[i];
		}
	}

	// converts this seg to a series of bytes
	public int makePayload(byte[] payload) {
		// add header 
		Utility.intToByte(seqNum, payload, SEQ_NUM_OFFSET);
		Utility.intToByte(ackNum, payload, ACK_NUM_OFFSET);
		Utility.intToByte(flags, payload, FLAGS_OFFSET);
		Utility.intToByte(checksum, payload, CHECKSUM_OFFSET);
		Utility.intToByte(rcvWin, payload, RCV_WIN_OFFSET);
		Utility.intToByte(length, payload, LENGTH_OFFSET);
		//add data
		for (int i=0; i<length; i++)
			payload[i+HDR_SIZE] = data[i];

		return HDR_SIZE + length;
	}

	public void setTimeoutHandler(RDTBuffer segBuf, DatagramSocket socket, InetAddress ip, int port){
		TimeoutHandler timeout = new TimeoutHandler(segBuf, this, socket, ip, port);
		if(timeoutHandler != null) timeoutHandler.cancel();
		timeoutHandler = timeout;
		RDT.timer.schedule(timeout, RDT.RTO);
	}

	public String toString(){
		return "SeqNum: " + seqNum + " ackNum: " + ackNum + " flags: " +  flags + " checksum: " + checksum +
				" rcvWin: " + rcvWin + " length: " + length + " Data: " + Arrays.toString(data);
	}

	public void printHeader() {
		System.out.println("SeqNum: " + seqNum);
		System.out.println("ackNum: " + ackNum);
		System.out.println("flags: " +  flags);
		System.out.println("checksum: " + checksum);
		System.out.println("rcvWin: " + rcvWin);
		System.out.println("length: " + length);
	}
	public void printData() {
		System.out.println("Data ... ");
		for (int i=0; i<length; i++) 
			System.out.print(data[i]);
		System.out.println(" ");
	}
	public void dump() {
		printHeader();
		printData();
	}
	
} // end RDTSegment class
