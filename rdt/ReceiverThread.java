/**
 * @author Benjamin Atbi
 *
 */

package rdt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

            if(!seg.isValid()){
                System.out.println("Received invalid segment");
                continue;
            }

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
                SRReceive(seg);
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

    private void SRReceive(RDTSegment seg){
        System.out.printf("SR Receive: seqNum %d \n", seg.seqNum, rcvBuf.next);

        rcvBuf.putSeqNum(seg);
        if(seg.seqNum < rcvBuf.base+rcvBuf.size) {
            RDTSegment ack = RDTSegment.createAckSegment(seg.seqNum);
            Utility.udp_send(ack, socket, dst_ip, dst_port);
        }
    }

    private void Ack(RDTSegment seg){
        System.out.println("Processing ACK");
        switch(RDT.protocol){
            case RDT.GBN:
                GBNAck(seg);
                break;
            case RDT.SR:
                SRAck(seg);
                break;
            default:
                System.out.println("Error in ReceiverThread ACK: unknown protocol");
        }
    }

    private void GBNAck(RDTSegment ack){

        //for each segment in send window, if sequence number is <= ACK, release it
        while(sndBuf.base <= ack.ackNum){
            System.out.printf("GBN ACK: ackNum %d base %d\n", ack.ackNum, sndBuf.base);
            RDTSegment seg = sndBuf.getBase();
            if(seg != null && seg.timeoutHandler != null){
                seg.timeoutHandler.cancel();
            }
        }

        //reset timer for new base
        RDTSegment new_base = sndBuf.accessBase();
        if(new_base != null){
            new_base.setTimeoutHandler(sndBuf, socket, dst_ip, dst_port);
        }

    }

    private void SRAck(RDTSegment ack){

        RDTSegment seg = sndBuf.accessSeqNum(ack.ackNum);
        seg.timeoutHandler.cancel();
        seg.ackReceived = true;

        //release contiguous acked packets
        RDTSegment base = sndBuf.accessBase();
        while(base != null && base.ackReceived){
            sndBuf.getBase();
            base = sndBuf.accessBase();
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
