package com.github.leasedlock;

/**
 * TODO: docs
 * 
 * @author gaurav
 */
public interface LockService {

  // Create and acquire lock
  Lock lock(String lockedEntityKey, long leaseExpirationMillis, String owner);

  // Release a previously acquired lock
  boolean unlock(String lockedEntityKey, String lockId);

  // Report who owns the lock
  String getOwner(String lockedEntityKey);

  // TODO: a simple LockServiceBuilder
}
