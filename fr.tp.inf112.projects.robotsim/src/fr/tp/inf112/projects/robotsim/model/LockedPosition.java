package fr.tp.inf112.projects.robotsim.model;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockedPosition extends Position implements Serializable {

	private static final long serialVersionUID = 7274819087013715987L;

	private final ReentrantLock lock = new ReentrantLock();
	
	public LockedPosition(final int xCoordinate, 
					final int yCoordinate) {
		super(xCoordinate, yCoordinate);
	}
	
	public LockedPosition() {
	    this(0, 0);
	}

	public Lock getLock() {
		return lock;
	}
	
	public void lock() {
	    lock.lock();
	}
	
	public boolean tryLock() {
	    return lock.tryLock();
	}

	public void unlock() {
	    lock.unlock();
	}
}
