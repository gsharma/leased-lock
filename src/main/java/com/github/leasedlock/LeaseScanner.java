package com.github.leasedlock;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Supervisor whose sole reason for existence is to unlock expired locks.
 * 
 * @author gaurav
 */
final class LeaseScanner extends Thread {
  private static final Logger logger = LogManager.getLogger(LeaseScanner.class.getSimpleName());

  // what to scan?
  private final ConcurrentMap<String, LeasedLock> lockPool;
  // how frequently/when to scan?
  private final long scanIntervalMillis;

  LeaseScanner(final ConcurrentMap<String, LeasedLock> lockPool, final long scanIntervalMillis) {
    setName("lock-lease-scanner");
    this.lockPool = lockPool;
    this.scanIntervalMillis = scanIntervalMillis;
    setDaemon(true);
  }

  @Override
  public void run() {
    // scan the list of locks
    // if lease is expiring:
    // 0. check if the lock is still there in list
    // 1. unlock the lock, if needed
    // 2. purge from internal structs
    //
    // note that might need to short circuit the path
    while (!isInterrupted()) {
      logger.info("Lease Scanner woke up to scan for lease expirations");
      for (final Entry<String, LeasedLock> lockEntry : lockPool.entrySet()) {
        final LeasedLock lock = lockEntry.getValue();
        if (lock != null && lock.isLocked() && lock.isExpired()) {
          // boolean released = lock.release(1);
          boolean released = lock.unlock();
          boolean purged = false;
          if (released) {
            purged = lockPool.remove(lockEntry.getKey(), lockEntry.getValue());
          }
          logger.info("released " + lock + ", released: " + released + ", purged: " + purged);
        }
      }
      try {
        sleep(scanIntervalMillis);
      } catch (InterruptedException e) {
        interrupt();
      }
    }
  }
}
