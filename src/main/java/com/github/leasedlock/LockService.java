package com.github.leasedlock;

/**
 * TODO: docs
 * 
 * @author gaurav
 */
public interface LockService {

  // Create a lock
  String createLock(long leaseExpirationMillis);

  // Acquire a previously created lock
  boolean lock(String lockId);

  // Release a previously acquired lock
  boolean unlock(String lockId);

  // Report if a lock associated with a given lock handle is locked
  boolean isLocked(String lockId);

  // Report who acquired the lock
  Thread getAcquirer(String lockId);

  // Report who released the lock
  Thread getReleaser(String lockId);

  // TODO: a simple LockServiceBuilder
}
