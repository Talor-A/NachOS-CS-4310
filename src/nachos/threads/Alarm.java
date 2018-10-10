package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;

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
	
	private static Lock alarmLock;
	private static Condition2 condition;
	private ArrayList<Long> timeList;
	private ArrayList<KThread> threadList;
	
    public Alarm() {
    	
    alarmLock = new Lock();
    condition = new Condition2(alarmLock);
    timeList = new ArrayList<Long>();
    threadList = new ArrayList<KThread>();
    
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
    	boolean machineStatus = Machine.interrupt().disable();
    	
    	//look through the entire list of times (and threads)
    	for (int i = 0; i < timeList.size(); i++)
    	{
    		//if a thread has waited long enough, wake it up and remove it from the queues
    		if(Machine.timer().getTime() > timeList.get(i))
    		{
    			KThread threadToBeWoken = threadList.remove(i);
    			alarmLock.acquire();
    			condition.wakeThisThread(threadToBeWoken);
    			alarmLock.release();
    			timeList.remove(i);
    		}
    	}
    		
    	Machine.interrupt().restore(machineStatus);
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
    	boolean machineStatus = Machine.interrupt().disable();
    	
    	//time must advance from now to now + x
    	long timeUntilWakingUp = Machine.timer().getTime() + x;
    	
    	//if time has already advanced, don't bother sleeping the current thread
    	if (Machine.timer().getTime() < timeUntilWakingUp)
    	{
    		//sleep the current thread
    		alarmLock.acquire();
    		condition.sleepThread();
    		alarmLock.release();
    		
    		//add both the thread and the time to a list
    		timeList.add(x);
    		threadList.add(KThread.currentThread());
    	}
    	
    	Machine.interrupt().restore(machineStatus);
    }
}
