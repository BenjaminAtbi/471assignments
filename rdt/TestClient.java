/**
 * @author mohamed
 *
 */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestClient {

	/**
	 * 
	 */
	public TestClient() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 if (args.length != 3) {
	         System.out.println("Required arguments: dst_hostname dst_port local_port");
	         return;
	      }
		 String hostname = args[0];
	     int dst_port = Integer.parseInt(args[1]);
	     int local_port = Integer.parseInt(args[2]);

		 scenario1(hostname,dst_port,local_port);
	}

	public static void scenario1(String hostname, int dst_port, int local_port){

		RDT rdt = new RDT(hostname, dst_port, local_port, 1, 3, RDT.GBN);
		RDT.setLossRate(0.0);

		byte[] buf = new byte[RDT.MSS];
		byte[] data = new byte[20];

		for (byte i = 0; i < 10; i++){
			for (int k=0; k<20; k++)
				data[k] = i;
			rdt.send(data, 20);
		}

		System.out.println(":Client has sent all data " );
		System.out.flush();

		rdt.receive(buf, RDT.MSS);
		rdt.close();
		System.out.println("Client is done " );
	}

	public static void scenario2(String hostname, int dst_port, int local_port){

		RDT rdt = new RDT(hostname, dst_port, local_port, 3, 3, RDT.GBN);
		RDT.setLossRate(0.0);

		byte[] buf = new byte[RDT.MSS];
		byte[] data = new byte[10];

		for (byte i = 0; i < 10; i++){
			for (int k=0; k<10; k++)
				data[k] = i;
			rdt.send(data, 10);
		}

		System.out.println(System.currentTimeMillis() + ":Client has sent all data " );
		System.out.flush();

		rdt.receive(buf, RDT.MSS);
		rdt.close();
		System.out.println("Client is done " );
	}
}
