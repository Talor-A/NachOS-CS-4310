package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	
	Lock lock;
	Condition2 conditionSpeak;
	Condition2 conditionListen;
	int numberOfListeners;
	LinkedList<Integer> queue;
	
    /**
     * Allocate a new communicator.
     */
    public Communicator() 
    {	
    	lock = new Lock();
    	conditionSpeak = new Condition2(lock);
    	conditionListen = new Condition2(lock);
    	numberOfListeners = 0;
    	queue = new LinkedList<Integer>();
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	
    	lock.acquire();
    	boolean machineStatus = Machine.interrupt().disable();
    	
    	queue.add(word);
    	//nobody is listening
    	if (numberOfListeners == 0)
    	{
    	    conditionSpeak.sleep();
    	}
    	
    	//now there is a listener!
    	assert(numberOfListeners > 0);
    	conditionListen.wake();
    	
    	Machine.interrupt().restore(machineStatus);
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {

    	lock.acquire();
    	numberOfListeners++;
    	boolean machineStatus = Machine.interrupt().disable();
    	
    	//nobody is speaking, wait until there is a speaker
    	if (queue.isEmpty())
    	{
    	    conditionListen.sleep();
    	}
    	
    	
    	conditionSpeak.wake();
    	assert(!queue.isEmpty());
    	
    	int value = queue.removeFirst();
    	lock.release();
    
    	numberOfListeners--;
    	Machine.interrupt().restore(machineStatus);
    	
    	return value;//queue.removeFirst();
    }
}
