package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	 
	/**
		Use condition variables and two lists, one to list the system time until a thread
		can awaken and one to list the threads that will be awakened at that time
	*/

	//create a priorityQueue to hold Data objects (see private inner class Data at bottom of file)
	private PriorityQueue<Data> priorityQueue;
	
    public Alarm() {
    	
	//initialize the priority queue
    priorityQueue = new PriorityQueue<Data>();
    
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt()
    {
    	//if a thread has waited long enough, wake it up and remove it from the priority queue.
    	//keep doing this until the queue is empty or the threads remaining in the queue must wait longer
    	while(!priorityQueue.isEmpty() && (Machine.timer().getTime() > priorityQueue.peek().timeToWake))
    	{
    		Data encapsulate = priorityQueue.remove(); //get the Data object from the priority queue
    		Condition condition = encapsulate.condition; //get the condition object from the Data object
    		Lock lock = encapsulate.lock; //get the lock from the Data object
    		lock.acquire();
    		condition.wake(); //wake up the thread to be awoken
    		lock.release();
    	}
    		
    	KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x)
    {
    	//time must advance from now to now + x
    	long timeToWake = Machine.timer().getTime() + x;
    	
    	//create a new lock and a new condition2 each time a thread calls this method
    	Lock lock = new Lock();
    	Condition condition = new Condition(lock);
    	
    	//if time has already advanced, don't bother sleeping the current thread
    	//otherwise, this thread must sleep
    	if (Machine.timer().getTime() < timeToWake)
    	{
    		lock.acquire();
    		
    		//add the condition, time to wake up, and lock to the priority queue
    		priorityQueue.add(new Data(condition, timeToWake, lock));
    		
    		//sleep the current thread
    		condition.sleep();
    		lock.release();
    	}
    }
    
    /**
     * A private inner class that is used to encapsulate data for the priority queue.
     * This class takes a condition2, lock, and time in for the constructor
     */
    private class Data implements Comparable<Data>
    {
    	Condition condition;
    	long timeToWake;
    	Lock lock;
    	
    	public Data(Condition thisCondition, long timeToWakeUp, Lock thisLock)
    	{
    		condition = thisCondition;
    		timeToWake = timeToWakeUp;
    		lock = thisLock;
    	}
    	
    	//override compareTo method, sort based upon times to wake
    	public int compareTo(Data other)
    	{
    		if (this.timeToWake > other.timeToWake)
    		{
    			return 1;
    		}
    		else if (this.timeToWake < other.timeToWake)
    		{
    			return -1;
    		}
    		else
    		{
    			return 0;
    		}
    	}
    }
}