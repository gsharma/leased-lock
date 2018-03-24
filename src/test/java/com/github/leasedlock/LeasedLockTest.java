package com.github.leasedlock;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests to maintain the sanity and correctness of LockService.
 * 
 * @author gaurav
 */
public class LeasedLockTest {
  private final LockService lockService = new LockServiceImpl();

  static {
    System.setProperty("log4j.configurationFile", "log4j.properties");
  }

  @Test
  public void testLockHappyPaths() throws Exception {
    // test auto-release of expired locks
    final Lock lock1 = lockService.lock("entity1", 400L, "owner1");
    assertNotNull(lock1.getId());
    assertEquals(lock1.getOwner(), lockService.getOwner(lock1.getLockedEntityKey()));
    assertTrue(lock1.isLocked());

    final Lock lock2 = lockService.lock("entity2", 700L, "owner2");
    assertNotNull(lock2.getId());
    assertEquals(lock2.getOwner(), lockService.getOwner(lock2.getLockedEntityKey()));
    assertTrue(lock2.isLocked());

    Thread.sleep(800L);

    // lease scanner should have done its job
    assertNull(lockService.getOwner(lock1.getLockedEntityKey()));
    assertNull(lockService.getOwner(lock2.getLockedEntityKey()));

    // test owner release of lock
    final Lock lock3 = lockService.lock("entity3", 500000L, "owner3");
    assertNotNull(lock3.getId());
    assertEquals(lock3.getOwner(), lockService.getOwner(lock3.getLockedEntityKey()));
    assertTrue(lock3.isLocked());

    // another owner trying to acquire someone else's lock
    final Lock acquireLockHeldByDifferentOwner =
        lockService.lock(lock3.getLockedEntityKey(), lock3.getLeaseExpirationMillis(), "owner4");
    assertNull(acquireLockHeldByDifferentOwner);

    // same owner trying to reacquire non-expired. already held lock
    final Lock tryReacquireAlreadyHeldLock = lockService.lock(lock3.getLockedEntityKey(),
        lock3.getLeaseExpirationMillis(), lock3.getOwner());
    assertNotNull(tryReacquireAlreadyHeldLock);

    assertTrue(lockService.unlock(lock3.getLockedEntityKey(), lock3.getId()));
    assertNull(lockService.getOwner(lock3.getLockedEntityKey()));
  }

}
