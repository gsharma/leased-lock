package com.github.leasedlock;

import java.util.concurrent.atomic.AtomicLong;

public class InMemorySequenceNumberGenerator implements SequenceNumberGenerator {
  private final AtomicLong sequenceNumberGenerator = new AtomicLong(6011);

  public long next() {
    return sequenceNumberGenerator.incrementAndGet();
  }
}
