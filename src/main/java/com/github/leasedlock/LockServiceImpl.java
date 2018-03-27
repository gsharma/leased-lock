package com.github.leasedlock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
 * @author gaurav
 */
final class LockServiceImpl implements LockService {
  private static final Logger logger = LogManager.getLogger(LockServiceImpl.class.getSimpleName());

  // core scan data struct
  private final ConcurrentMap<String, Lock> lockPool = new ConcurrentHashMap<>();

  // TODO: perennial locks data structure. These locks never expire, so, no point paying the price
  // to scan them in the original list
  private final Map<String, LockServiceImpl> perennialLocks = new HashMap<>();

  // TODO: this should be configurable as a local or remote sequence generator
  private final SequenceNumberGenerator sequenceNumberGenerator =
      new InMemorySequenceNumberGenerator();

  // TODO: remove silly hardcoding
  LockServiceImpl(final long scanIntervalMillis) {
    new LeaseScanner(lockPool, scanIntervalMillis).start();
  }

  // TODO: maintain a low watermark for lease expiration to avoid running
  // the expiration scanner like a mad daemon
  @Override
  public Lock lock(final String lockedEntityKey, final long leaseExpirationMillis,
      final String owner) {
    final ReentrantLeasedLock lock = new ReentrantLeasedLock(lockedEntityKey, leaseExpirationMillis,
        sequenceNumberGenerator.next(), owner);
    boolean locked = lock.lock();
    Lock candidate = null;
    if (locked) {
      candidate = lockPool.putIfAbsent(lockedEntityKey, lock);
      if (candidate != null && !candidate.getOwner().equals(lock.getOwner())) {
        candidate = null;
        logger.info(owner + " failed to acquire lock on " + lockedEntityKey);
      } else {
        if (candidate == null) {
          logger.info("acquired " + lock);
        } else {
          logger.info(owner + " already acquired lock on " + lockedEntityKey);
        }
        candidate = lock;
      }
    }
    return candidate;
  }

  @Override
  public boolean unlock(final String lockedEntityKey, final String lockId, String owner) {
    boolean unlocked = false;
    Lock lock = lockPool.get(lockedEntityKey);
    if (lock != null && lock.getId().equals(lockId) && lock.getOwner().equals(owner)) {
      unlocked = ReentrantLeasedLock.class.cast(lock).unlock();
      if (unlocked) {
        lock = lockPool.remove(lockedEntityKey);
        if (lock != null) {
          logger.info("released " + lock);
          unlocked = true;
        } else {
          unlocked = false;
        }
      }
    }
    return unlocked;
  }

  @Override
  public String getOwner(String lockedEntityKey) {
    String owner = null;
    final Lock lock = lockPool.get(lockedEntityKey);
    if (lock != null) {
      owner = lock.getOwner();
    }
    return owner;
  }

}
