package com.github.leasedlock;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
public final class ReentrantLeasedLock implements Lock {
  private static final Logger logger =
      LogManager.getLogger(ReentrantLeasedLock.class.getSimpleName());

  // lock's own id which is surrogate but mostly useful for debugging
  private final String id = UUID.randomUUID().toString();

  /**
   * user-provided attribute: metadata string representative of the entity that is being locked.
   * Note that for our purposes of locking, this is opaque to the service and we could care less if
   * it is say "foobar" or "0000" or something else.
   */
  private final String lockedEntityKey;

  /**
   * user-provided attribute: expiration time of the lease.
   */
  private final long leaseExpirationMillis;

  /**
   * user-provided attribute: owner of this lock. Note that this differs from acquirerThread which
   * is an internal-only attribute for our house-keeping and tracking purposes.
   */
  private final String owner;

  // fencing token useful for ordering operations performed using the lock
  private final long fencingToken;

  private final AtomicBoolean locked = new AtomicBoolean();

  private final long createdTstamp;
  private long acquiredTstamp;
  private long releasedTstamp;
  private long leaseHeldMillis;

  private Thread acquirerThread;
  private Thread releaserThread;

  // TODO: check valid input data
  ReentrantLeasedLock(final String lockedEntityKey, final long leaseExpirationMillis,
      final long fencingToken, final String owner) {
    this.lockedEntityKey = lockedEntityKey;
    this.leaseExpirationMillis = leaseExpirationMillis;
    this.owner = owner;
    this.fencingToken = fencingToken;
    createdTstamp = System.currentTimeMillis();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getLockedEntityKey() {
    return lockedEntityKey;
  }

  @Override
  public long getLeaseExpirationMillis() {
    return leaseExpirationMillis;
  }

  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public long getFencingToken() {
    return fencingToken;
  }

  @Override
  public boolean isExpired() {
    boolean expired = false;
    long expirationMillis = getLeaseHeldMillis() - leaseExpirationMillis;
    expired = expirationMillis >= 0;
    if (expired) {
      logger.info("expired alread " + toString() + ", expirationMillis: " + expirationMillis);
    }
    return expired;
  }

  boolean unlock() {
    if (acquirerThread != Thread.currentThread()
        && Thread.currentThread().getClass() != LeaseScanner.class) {
      throw new UnsupportedOperationException("Leased lock can only by unlocked by its owner");
    }
    boolean released = false;
    if (locked.compareAndSet(true, false)) {
      released = true;
      releasedTstamp = System.currentTimeMillis();
      releaserThread = Thread.currentThread();
    }
    return released;
  }

  @Override
  public boolean isLocked() {
    return locked.get();
  }

  // acquires the lock if state is zero
  boolean lock() {
    boolean acquired = false;
    if (locked.compareAndSet(false, true)) {
      acquiredTstamp = System.currentTimeMillis();
      acquirerThread = Thread.currentThread();
      acquired = true;
    }
    return acquired;
  }

  // if the lock is not yet acquired or acquired but not yet released, this returns -1
  @Override
  public long getLeaseHeldMillis() {
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

  // TODO: this could also be a node in case of distributed locks
  Thread getAcquirer() {
    return acquirerThread;
  }

  // TODO: this could also be a node in case of distributed locks
  Thread getReleaser() {
    return releaserThread;
  }

  @Override
  public String toString() {
    return "Lock [id:" + id + ", lockedEntityKey:" + lockedEntityKey + ", owner:" + owner
        + ", token:" + fencingToken + ", ttl:" + leaseExpirationMillis + ", acquirer:"
        + (acquirerThread != null ? acquirerThread.getName() : null) + ", releaser:"
        + (releaserThread != null ? releaserThread.getName() : null) + ", created:" + createdTstamp
        + ", acquired:" + acquiredTstamp + ", released:" + releasedTstamp + "]";
  }

}
