package com.github.leasedlock;

/**
 * TODO: docs
 * 
 * @author gsharma1
 */
public interface LockService {

  String createLock(long leaseExpirationMillis);

  boolean lock(String lockId);

  boolean unlock(String lockId);

  boolean isLocked(String lockId);

  Thread getAcquirer(String lockId);

  Thread getReleaser(String lockId);

}
