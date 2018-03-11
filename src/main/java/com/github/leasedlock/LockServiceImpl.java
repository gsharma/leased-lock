package com.github.leasedlock;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Leased lock implementation. This allows a supervisor thread to release locks that were acquired
 * by other threads.
 * 
 * on lock:<br>
 * 1. create a lock<br>
 * 2. add it to registry<br>
 * 3. push it to heap<br>
 * 4. commit - mark creation complete<br>
 * 
 * on unlock or expiration:<br>
 * 5. lookup lock from registry, don't delete it yet<br>
 * 6. delete lock from heap<br>
 * 7. delete lock from registry<br>
 * 8. commit - mark unlock complete<br>
 * 
 * Three options are typically available for gating:<br>
 * 1. lock acquisition succeeds<br>
 * 2. lock acquisition blocks<br>
 * 3. lock acquisition fails<br>
 * 
 * @author gsharma1
 */
public final class LockServiceImpl implements LockService {
  private static final Logger logger = LogManager.getLogger(LockServiceImpl.class.getSimpleName());

  // core scan data struct
  // 0. start with a simpler list, maybe be a deque
  // 1. maintain a min-heap of lock ttl (not needed initially)
  private final Queue<LeasedLock> lockPool = new LinkedList<>();

  // TODO: perennial locks data structure. These locks never expire, so, no point paying the price
  // to scan them in the original list
  private final Map<String, LockServiceImpl> perennialLocks = new HashMap<>();

  // TODO: remove silly hardcoding
  public LockServiceImpl() {
    new LeaseScanner(lockPool, 100L).start();
  }

  // TODO: maintain a low watermark for lease expiration to avoid running
  // the expiration scanner like a mad daemon
  @Override
  public String createLock(final long leaseExpirationMillis) {
    final LeasedLock lock = new LeasedLock(leaseExpirationMillis);
    logger.info("created " + lock);
    lockPool.add(lock);
    return lock.getId();
  }

  @Override
  public boolean lock(final String lockId) {
    boolean locked = false;
    LeasedLock lock = locate(lockId);
    if (lock != null) {
      locked = lock.lock();
      logger.info("acquired " + lock);
    }
    return locked;
  }

  @Override
  public boolean unlock(final String lockId) {
    boolean unlocked = false;
    LeasedLock lock = locate(lockId);
    if (lock != null) {
      unlocked = lock.unlock();
      if (unlocked) {
        lockPool.remove(lock);
        logger.info("released " + lock);
      }
    }
    return unlocked;
  }

  @Override
  public boolean isLocked(final String lockId) {
    boolean locked = false;
    LeasedLock lock = locate(lockId);
    if (lock != null) {
      locked = lock.isLocked();
    }
    return locked;
  }

  @Override
  public Thread getAcquirer(String lockId) {
    Thread acquirer = null;
    LeasedLock lock = locate(lockId);
    if (lock != null) {
      acquirer = lock.getAcquirer();
    }
    return acquirer;
  }

  @Override
  public Thread getReleaser(String lockId) {
    Thread releaser = null;
    LeasedLock lock = locate(lockId);
    if (lock != null) {
      releaser = lock.getReleaser();
    }
    return releaser;
  }

  private LeasedLock locate(final String lockId) {
    for (final LeasedLock lock : lockPool) {
      if (lock != null && lock.getId().equals(lockId)) {
        return lock;
      }
    }
    return null;
  }

}
