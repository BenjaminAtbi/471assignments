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

    RDTBuffer (int bufSize) {
        buf = new RDTSegment[bufSize];
        for (int i=0; i<bufSize; i++)
            buf[i] = null;
        size = bufSize;
        base = next = 0;
        semMutex = new Semaphore(1, true);
        semFull =  new Semaphore(0, true);
        semEmpty = new Semaphore(bufSize, true);
    }

    // Put a segment in the next available slot in the buffer
    public void putNext(RDTSegment seg) {
//        dump("putnext");
        try {
            semEmpty.acquire(); // wait for an empty slot
            semMutex.acquire(); // wait for mutex

            buf[next%size] = seg;
            next++;

            semMutex.release();
            semFull.release(); // increase #of full slots
        } catch(InterruptedException e) {
            System.out.println("Buffer put(): " + e);
        }
    }

    // return the next in-order segment
    public RDTSegment getBase() {
//        dump("getbase");
        try {
            semFull.acquire();  // wait for full slot
            semMutex.acquire(); // wait for mutex

            RDTSegment seg = buf[base%size];
            base++;

            semMutex.release();
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

            //shortcut if buffer is empty
            if(base >= next){
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

    public ArrayList<RDTSegment> accessActive() {
//        dump("accessactive");
        try {
            semMutex.acquire();
            ArrayList<RDTSegment> segments = new ArrayList<>();
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

    // Put a segment in the *right* slot based on seg.seqNum
    // used by receiver in Selective Repeat
    public void putSeqNum (RDTSegment seg) {
        // ***** compelte

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