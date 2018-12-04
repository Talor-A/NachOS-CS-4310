package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     * transfer priority from waiting threads
     * to the owning thread.
     * @return a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityThreadQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param    thread    the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityThreadQueue extends ThreadQueue
    {
        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;

        protected PriorityQueue<ThreadState> priorityQueue = new PriorityQueue<ThreadState>();
        
        ThreadState owningThread = null;
        

        PriorityThreadQueue(boolean transferPriority)
        {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread)
        {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread)
        {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread()
        {
            Lib.assertTrue(Machine.interrupt().disabled());
            
            // implement me
            
            ThreadState nextThread = priorityQueue.poll(); //we're taking this from the front of the queue
            
            if (nextThread == null)
            {
            	return null;
            }
            
            //there is a donation that must occur
            if (transferPriority)
            {
            	owningThread.release(this);
            	nextThread.waitingQueue = null;
            	nextThread.enqueue(this);
            }
            
            owningThread = nextThread;
            return nextThread.thread; //return the thread associated with this ThreadState
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         * return.
         */
        protected ThreadState pickNextThread()
        {
            // implement me
            return priorityQueue.peek(); //the front of the priority queue holds the next thread to be scheduled
        }

        public void print()
        {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }

        //allows a ThreadState to add threads to the priority queue of a PriorityThreadQueue object
        public void add(ThreadState thread)
        {
            priorityQueue.add(thread);
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see    nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState>
    {

        protected KThread thread;
        
        protected int priority = priorityDefault;
        
        public long startedWaiting; //to hold how long the thread has been waiting
        
        protected LinkedList<PriorityThreadQueue> acquiredQueues;
        
        protected int effectivePriority;
        
        //If there is a donation, capture the priority queue the ThreadState is in
        protected PriorityThreadQueue waitingQueue = null;

        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param    thread    the thread this state belongs to.
         */
        public ThreadState(KThread thread)
        {
            this.thread = thread;
            setPriority(priorityDefault);
            effectivePriority = priorityDefault;
            acquiredQueues = new LinkedList<PriorityThreadQueue>();
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority()
        {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority()
        {
            // implement me
            return effectivePriority;
        }

        private void calculateEffectivePriority()
        {
        	int maxPriority = -1; //begin with lowest priority, get the greatest of the threads waiting on you
        	
        	//loop through all threads in the queue. If there is a donation and the thread has a higher priority
        	//than the current thread, a donation must occur. Raise the effective priority of this ThreadState
        	if (!acquiredQueues.isEmpty())
        	{
        		for (int i = 0; i < acquiredQueues.size(); i++)
        		{
        			PriorityThreadQueue queue = acquiredQueues.get(i);
        			ThreadState nextThread = queue.pickNextThread();
        			
        			if (nextThread != null)
        			{
        				if ((nextThread.getEffectivePriority() > maxPriority) && queue.transferPriority)
						{
							maxPriority = nextThread.getEffectivePriority();
						}
        			}
        		}
        	}
        	
        	//the greatest effective priority is the current priority
        	if (this.getPriority() > maxPriority) maxPriority = this.getPriority();
        	
        	effectivePriority = maxPriority;
        	
        	//recalculate the effective priority of the ThreadState if it doesn't match that of
        	//the ThreadState that owns the resource guarded by the ThreadQueue
        	if (waitingQueue != null && waitingQueue.owningThread != null)
        	{
        		if (effectivePriority != waitingQueue.owningThread.effectivePriority)
        		{
        		    // if this priority has changed, we should check if the top thread should be updated
        		    waitingQueue.owningThread.calculateEffectivePriority();
        		}
        	}
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param    priority    the new priority.
         */
        public void setPriority(int priority)
        {
            if (this.priority == priority)
                return;

            this.priority = priority;
            
            // implement me

            calculateEffectivePriority();
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param    waitQueue    the queue that the associated thread is
         * now waiting on.
         * @see    nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityThreadQueue waitQueue)
        {
            startedWaiting = Machine.timer().getTime(); //set the time the thread started waiting for access
            waitQueue.add(this); //add the thread to the priority queue
            waitingQueue = waitQueue; //save the priority queue
            calculateEffectivePriority();
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see    nachos.threads.ThreadQueue#acquire
         * @see    nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityThreadQueue waitQueue)
        {
            // implement me
        	
        	waitQueue.owningThread = this;
            acquiredQueues.add(waitQueue);
        }
        
        public void enqueue(PriorityThreadQueue waitQueue)
        {
        	acquiredQueues.add(waitQueue);
        	calculateEffectivePriority();
        }

        public void release(PriorityThreadQueue waitQueue)
        {
            acquiredQueues.remove(waitQueue);
            calculateEffectivePriority();
        }

        //sort first according to the effective priority of the threads
        //if the priority is the same, then sort based upon which thread
        //has been waiting longer
        @Override
        public int compareTo(ThreadState other)
        {
            if (this.effectivePriority > other.effectivePriority)
            {
                return -1; //put it towards the front of the queue, it has a higher priority
            }

            if (this.effectivePriority < other.effectivePriority)
            {
                return 1; //put it towards the end of the queue, it has a lower priority
            }

            // priorities are equal
            if (this.startedWaiting < other.startedWaiting)
            {
                return -1; //put it towards the front of the queue, it has been waiting longer
            }

            //this.waitStartTime > other.waitStartTime
            return 1; //put it towards the end of the queue, it has not been waiting as long
        }
    }
}