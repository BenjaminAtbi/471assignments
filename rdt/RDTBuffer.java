/**
 * @author Benjamin Atbi
 *
 */

package rdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

class RDTBuffer {
    public RDTSegment[] buf;
    public int size;
    public int base;
    public int next;
    public Semaphore semMutex; // for mutual execlusion
    public Semaphore semFull; // #of full slots
    public Semaphore semEmpty; // #of Empty slots
    public Semaphore semBase; // base slot is full

    RDTBuffer (int bufSize) {
        buf = new RDTSegment[bufSize];
        for (int i=0; i<bufSize; i++)
            buf[i] = null;
        size = bufSize;
        base = next = 0;
        semMutex = new Semaphore(1, true);
        semFull =  new Semaphore(0, true);
        semEmpty = new Semaphore(bufSize, true);
        semBase = new Semaphore(0, true);
    }

    // Put a segment in the next available slot in the buffer
    public void putNext(RDTSegment seg) {
//        dump("putnext");
        try {
            semEmpty.acquire(); // wait for an empty slot
            semMutex.acquire(); // wait for mutex

            buf[next%size] = seg;
            //if next just filled base, signal base
            if(next == base){
                semBase.release();
            }
            next++;

            semMutex.release();
            semFull.release(); // increase #of full slots
        } catch(InterruptedException e) {
            System.out.println("Buffer put next(): " + e);
        }
    }

    // Put a segment in the *right* slot based on seg.seqNum
    // used by receiver in Selective Repeat
    // MUTUALLY EXCLUSIVE with putNext function (don't use in same buffer)
    public void putSeqNum (RDTSegment seg) {
        try {
            semEmpty.acquire();
            semMutex.acquire();

            //shortcut if seg not within current window or spot is not empty
            // (Everything already in buffer should be the correct seg in the current window)
            if(seg.seqNum < base || seg.seqNum >= base+size || buf[seg.seqNum%size] != null) {
                semMutex.release();
                semEmpty.release();
                return;
            }

            buf[seg.seqNum%size] = seg;
            if(seg.seqNum == base){
                semBase.release();
            }

            semMutex.release();
            semFull.release();
        } catch(InterruptedException e) {
            System.out.println("Buffer put seq(): "+ e);
        }
    }

    public RDTSegment accessSeqNum(int seqNum) {
        try {
            semMutex.acquire();

            //shortcut if seq num not within current window
            if(seqNum < base || seqNum >= base+size) {
                semMutex.release();
                return null;
            }

            RDTSegment seg = buf[seqNum%size];
            semMutex.release();
            return seg;

        } catch(InterruptedException e) {
            System.out.println("Buffer access Seg(): " + e);
        }
        return null;
    }

    // return the next in-order segment
    public RDTSegment getBase() {
//        dump("getbase");
        try {
            semBase.acquire(); //wait for base to be full
            semFull.acquire();  // wait for full slot
            semMutex.acquire(); // wait for mutex

            RDTSegment seg = buf[base%size];
            buf[base%size] = null;
            base++;

            semMutex.release();
            //if new base is also full, signal it
            if(buf[base%size] != null){
                semBase.release();
            }
            semEmpty.release(); // increase empty slots
            return seg;
        } catch(InterruptedException e) {
            System.out.println("Buffer get(): " + e);
        }

        return null;  // fix
    }

    //access next in-order segment without removing
    public RDTSegment accessBase() {
//        dump("accessbase");
        try {
            semMutex.acquire();

            //shortcut if base segment is empty
            if(buf[base%size] == null){
                semMutex.release();
                return null;
            }

            RDTSegment seg = buf[base%size];
            semMutex.release();
            return seg;
        } catch(InterruptedException e) {
            System.out.println("Buffer access Base(): " + e);
        }
        return null;
    }

    //access all segments between base and next (some may be null)
    public ArrayList<RDTSegment> accessActive() {
//        dump("accessactive");
        try {
            semMutex.acquire();

            ArrayList<RDTSegment> segments = new ArrayList<>();

            //shortcut if used on a buffer without guaranteed Contiguousness
            if(next < base){
                semMutex.release();
                return segments;
            }

            for(int i = base; i != next; i++){
                segments.add(buf[i%size]);
            }
            semMutex.release();
            System.out.println("active segments: "+segments.toString());
            return segments;
        } catch(InterruptedException e) {
            System.out.println("Buffer access Base(): " + e);
        }
        return null;
    }

    // for debugging
    public void dump() {
        System.out.println("Dumping the buffer. size: "+size+" base: "+base+" baseIndex: "+(base%size)+
                " next: "+next+" nextIndex: "+(next%size)+" buffer: "+ Arrays.toString(buf));
    }

    public void dump(String opname) {
        System.out.println(opname+": Dumping the buffer. size: "+size+" semFull: "+semFull.availablePermits()+
                " semEmpty: "+semEmpty.availablePermits()+" base: "+base+" next: "+next+" buffer: "+ Arrays.toString(buf));
    }
} // end RDTBuffer class

