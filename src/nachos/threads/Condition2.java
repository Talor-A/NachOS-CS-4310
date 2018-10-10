package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt(s) for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
	
	private Lock conditionLock;
	private LinkedList<KThread> waitQueue; //used for a queue
	private LinkedList<KThread> threadList; //used to wake a specific thread
	
    public Condition2(Lock conditionLock)
    {
		threadList = new LinkedList<>();
		waitQueue = new LinkedList<KThread>();
    	this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically re-acquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep()
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	//add the current process to the linked list
    	conditionLock.release();
    	
    	boolean status = Machine.interrupt().disable();
    	waitQueue.add(KThread.currentThread()); //add the thread to the queue, it will be waiting to be woken up
		
		//sleep, waiting for wake() to be called
		KThread.sleep();
		
		Machine.interrupt().restore(status);

		conditionLock.acquire(); //get the lock back
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake()
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	if (!waitQueue.isEmpty()) //if the queue is empty, there is nothing to wake
    	{
    		boolean status = Machine.interrupt().disable();
    		KThread thisThread = waitQueue.removeFirst(); //get the first thread that fell asleep
    		thisThread.ready(); //wake the first thread
    		Machine.interrupt().restore(status);
    	}
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll()
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	while (!waitQueue.isEmpty()) //simply call wake() for all sleeping threads
    	    wake();
    }
    
	//sleeps a thread, adding the thread to a linked list instead of a queue
    public void sleepThread()
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	conditionLock.release();
    	boolean status = Machine.interrupt().disable();
		
    	threadList.add(KThread.currentThread()); //add the thread to a list
    	KThread.sleep(); //sleep the thread
		
    	Machine.interrupt().restore(status);
    	conditionLock.acquire();
    }
    
	//wakes a specific thread from the linked list
    public void wakeThisThread(KThread threadToWake)
    {
    	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    	
    	conditionLock.release();
    	boolean status = Machine.interrupt().disable();
    	
		//don't wake a thread if there are none sleeping
    	if (!threadList.isEmpty())
    	{
			//iterate over all sleeping threads
    		for (int i = 0; i < threadList.size(); i++)
    		{
				//if the thread is the one to wake, wake it up
    			if (threadToWake.compareTo(threadList.get(i)) == 0)
    			{
    				threadList.remove(i).ready();
    			}
    		}
    	}
    	
    	Machine.interrupt().restore(status);
    }
}
