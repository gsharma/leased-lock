package com.github.leasedlock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO: docs
 * 
 * @author gaurav
 */
public interface LockService {

  // Create and acquire lock
  Lock lock(String lockedEntityKey, long leaseExpirationMillis, String owner);

  // Release a previously acquired lock
  boolean unlock(String lockedEntityKey, String lockId, String owner);

  // Report who owns the lock
  String getOwner(String lockedEntityKey);

  /**
   * A simple builder to let users use fluent APIs to build LockService.
   */
  public final static class LockServiceBuilder {
    private String namespace;
    private long scanIntervalMillis;

    public static LockServiceBuilder newBuilder() {
      return new LockServiceBuilder();
    }

    public LockServiceBuilder namespace(final String namespace) {
      this.namespace = namespace;
      return this;
    }

    public LockServiceBuilder scanIntervalMillis(long scanIntervalMillis) {
      this.scanIntervalMillis = scanIntervalMillis;
      return this;
    }

    public LockService build() {
      final LockService service = new LockServiceImpl(scanIntervalMillis);
      lockServiceRegistry.putIfAbsent(namespace, service);
      return service;
    }

    private LockServiceBuilder() {}
  }

  final static ConcurrentMap<String, LockService> lockServiceRegistry = new ConcurrentHashMap<>();

}
