/**
 * @author mhefeeda
 *
 */

package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

class TimeoutHandler extends TimerTask {
	RDTBuffer sndBuf;
	RDTSegment seg; 
	DatagramSocket socket;
	InetAddress ip;
	int port;
	
	TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, 
			InetAddress ip_addr, int p) {
		sndBuf = sndBuf_;
		seg = s;
		socket = sock;
		ip = ip_addr;
		port = p;
	}
	
	public void run() {
		
		System.out.println(System.currentTimeMillis()+ ":Timeout for seg: " + seg.seqNum);
		System.out.flush();
		
		// complete 
		switch(RDT.protocol){
			case RDT.GBN:
				RDTSegment new_base = sndBuf.accessBase();
				if(new_base != null){
					System.out.println(System.currentTimeMillis()+ ":Resetting Timeout for seg: " + new_base.seqNum);
					new_base.setTimeoutHandler(sndBuf, socket, ip, port);
				}

				for(RDTSegment resend_seg : sndBuf.accessActive()){
					System.out.println(System.currentTimeMillis()+ ":resending seg: " + resend_seg.seqNum);
					Utility.udp_send(resend_seg, socket, ip, port);
				}
				break;
			case RDT.SR:
				
				break;
			default:
				System.out.println("Error in TimeoutHandler:run(): unknown protocol");
		}
		
	}
} // end TimeoutHandler class

