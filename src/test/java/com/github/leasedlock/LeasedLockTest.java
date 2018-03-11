package com.github.leasedlock;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests to maintain the sanity and correctness of LockService.
 */
public class LeasedLockTest {
  private final LockService lockService = new LockServiceImpl();

  static {
    System.setProperty("log4j.configurationFile", "log4j.properties");
  }

  @Test
  public void testLockHappyPaths() throws Exception {
    // test auto-release of expired locks
    String lockId1 = lockService.createLock(400L);
    assertNotNull(lockId1);
    assertNull(lockService.getAcquirer(lockId1));
    assertNull(lockService.getReleaser(lockId1));
    assertFalse(lockService.isLocked(lockId1));
    assertTrue(lockService.lock(lockId1));
    assertTrue(lockService.isLocked(lockId1));
    assertNotNull(lockService.getAcquirer(lockId1));
    assertNull(lockService.getReleaser(lockId1));

    String lockId2 = lockService.createLock(700L);
    assertNotNull(lockId2);
    assertNull(lockService.getAcquirer(lockId2));
    assertNull(lockService.getReleaser(lockId2));
    assertFalse(lockService.isLocked(lockId2));
    assertTrue(lockService.lock(lockId2));
    assertTrue(lockService.isLocked(lockId2));
    assertNotNull(lockService.getAcquirer(lockId2));
    assertNull(lockService.getReleaser(lockId2));

    Thread.sleep(800L);

    // lease scanner should have done its job
    assertFalse(lockService.isLocked(lockId1));
    assertFalse(lockService.isLocked(lockId2));

    // test owner release of lock
    String lockId3 = lockService.createLock(500L);
    assertNotNull(lockId3);
    assertTrue(lockService.lock(lockId3));
    assertTrue(lockService.isLocked(lockId3));
    assertTrue(lockService.unlock(lockId3));
    assertFalse(lockService.isLocked(lockId3));
  }

}
