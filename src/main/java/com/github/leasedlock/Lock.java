package com.github.leasedlock;

public interface Lock {
  String getId();

  String getLockedEntityKey();

  long getFencingToken();

  boolean isExpired();

  long getLeaseExpirationMillis();

  boolean isLocked();

  long getLeaseHeldMillis();

  String getOwner();
}
