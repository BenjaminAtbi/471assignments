/**
 * @author Benjamin Atbi
 *
 */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestServer {

	public TestServer() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.out.println("Required arguments: dst_hostname dst_port local_port");
			return;
		}
		String hostname = args[0];
		int dst_port = Integer.parseInt(args[1]);
		int local_port = Integer.parseInt(args[2]);

		scenario1(hostname, dst_port, local_port);
	}

	private static void scenario1(String hostname, int dst_port, int local_port) {

		RDT rdt = new RDT(hostname, dst_port, local_port, 3, 5, RDT.SR);
		RDT.setLossRate(0.6);
		byte[] buf = new byte[500];
		System.out.println("Server is waiting to receive ... ");

		while (true) {
			int size = rdt.receive(buf, 500);
			System.out.println("Server Received:"+Arrays.toString(buf));
			System.out.println(" ");
			System.out.flush();
		}
	}
}

