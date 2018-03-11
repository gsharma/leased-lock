package com.github.leasedlock;

import java.util.UUID;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Core locking construct which is somewhere in between a mutex and a binary semaphore and a
 * counting semaphore of count 1. It allows the best of both worlds - mutex semantics for the
 * end-user and binary semaphore semantics internally to allow a supervisor thread to release it if
 * its lease has expired.
 * 
 * @author gaurav
 */
final class LeasedLock extends AbstractQueuedSynchronizer {
  private static final Logger logger = LogManager.getLogger(LeasedLock.class.getSimpleName());
  private static final long serialVersionUID = 1L;

  private final String id;

  private final long leaseExpirationMillis;
  private long leaseHeldMillis;
  private final long createdTstamp;
  private long acquiredTstamp;
  private long releasedTstamp;

  private Thread acquirerThread;
  private Thread releaserThread;

  LeasedLock(final long leaseExpirationMillis) {
    id = UUID.randomUUID().toString();
    createdTstamp = System.currentTimeMillis();
    this.leaseExpirationMillis = leaseExpirationMillis;
  }

  String getId() {
    return id;
  }

  boolean isExpired() {
    boolean expired = false;
    long expirationMillis = getLeaseHeldMillis() - leaseExpirationMillis;
    expired = expirationMillis >= 0;
    if (expired) {
      logger.info("expired already id: " + id + ", expirationMillis: " + expirationMillis);
    }
    return expired;
  }

  long getLeaseExpirationMillis() {
    return leaseExpirationMillis;
  }

  boolean unlock() {
    if (getAcquirer() != Thread.currentThread()
        && Thread.currentThread().getClass() != LeaseScanner.class) {
      throw new UnsupportedOperationException("Leased lock can only by unlocked by its owner");
    }
    boolean released = false;
    if (release(1)) {
      released = true;
      releaserThread = Thread.currentThread();
    }
    return released;
  }

  boolean isLocked() {
    return getState() == 1;
  }

  // acquires the lock if state is zero
  boolean lock() {
    boolean acquired = false;
    if (compareAndSetState(0, 1)) {
      acquiredTstamp = System.currentTimeMillis();
      setExclusiveOwnerThread(Thread.currentThread());
      acquirerThread = Thread.currentThread();
      acquired = true;
    }
    return acquired;
  }

  // if the lock is not yet acquired or acquired but not yet released, this returns -1
  long getLeaseHeldMillis() {
    // 1. active and acquired lock
    if (isLocked()) {
      leaseHeldMillis = System.currentTimeMillis() - acquiredTstamp;
    }
    // 2. never acquired lock
    else if (acquiredTstamp == 0L) {
      return -1;
    }
    // 3. possibly previously acquired lock, now released
    return leaseHeldMillis;
  }

  Thread getAcquirer() {
    return acquirerThread;
  }

  Thread getReleaser() {
    return releaserThread;
  }

  // Releases the lock by setting state to zero
  @Override
  protected boolean tryRelease(int releases) {
    if (getState() == 0) {
      // maybe log here?
      return false;
    }
    setExclusiveOwnerThread(null);
    setState(0);
    releasedTstamp = System.currentTimeMillis();
    return true;
  }

  @Override
  public String toString() {
    return "LeasedLock [id:" + id + ", ttl:" + leaseExpirationMillis + ", acquirer:"
        + (acquirerThread != null ? acquirerThread.getName() : null) + ", releaser:"
        + (releaserThread != null ? releaserThread.getName() : null) + ", created:" + createdTstamp
        + ", acquired:" + acquiredTstamp + ", released:" + releasedTstamp + "]";
  }

}
