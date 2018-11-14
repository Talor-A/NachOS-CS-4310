package nachos.threads;

import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	
	/**
		We need two condition variables, one to hold a queue or suspend all listeners,
		and one to hold a queue or suspend all speakers.
		
		We keep track of the number of listeners with an integer and the number of speakers
		indirectly using a LinkedList as a queue, which holds the speaker's words.
	*/
	Lock lock;
	Condition conditionSpeak;
	Condition conditionListen;
	int numberOfListeners;
	LinkedList<Integer> queue;
	
    /**
     * Allocate a new communicator.
     */
    public Communicator() 
    {	
		//initialize all the objects and variables
    	lock = new Lock();
    	conditionSpeak = new Condition(lock);
    	conditionListen = new Condition(lock);
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
    	
    	queue.add(word); //add the word to a queue of words to be listened to
		
    	//nobody is listening
    	if (numberOfListeners == 0)
    	{
    	    conditionSpeak.sleep(); //sleep the speakers until somebody is listening
    	}
    	
    	//now there is a listener!
    	assert(numberOfListeners > 0);
    	conditionListen.wake(); //wake a listener to hear the Good News!
    	
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
    	numberOfListeners++; //increment the number of listeners
    	
    	//nobody is speaking, wait until there is a speaker
    	while (queue.isEmpty())
    	{
    	    conditionListen.sleep(); //fall asleep until a speaker comes to wake the thread up
    	}
    	
    	conditionSpeak.wake(); //wake a speaker so the thread can listen to them
    	assert(!queue.isEmpty()); //there should now be a word to receive
    	
    	int value = queue.removeFirst(); //remove the word from the queue
    
    	numberOfListeners--; //decrement the number of listeners
    	lock.release(); //release the lock
    	
    	return value; //return the word
    }
}