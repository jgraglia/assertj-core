/*
 * Created on Oct 24, 2010
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright @2010 the original author or authors.
 */
package org.fest.assertions.internal;

import static org.fest.assertions.error.IsNotLessThanOrEqualTo.isNotLessThanOrEqualTo;
import static org.fest.assertions.test.ExpectedException.none;
import static org.fest.assertions.test.FailureMessages.unexpectedNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import org.fest.assertions.core.*;
import org.fest.assertions.test.ExpectedException;
import org.junit.*;

/**
 * Tests for <code>{@link Floats#assertLessThanOrEqualTo(AssertionInfo, Float, float)}</code>.
 *
 * @author Alex Ruiz
 */
public class Floats_assertLessThanOrEqualTo_Test {

  private static WritableAssertionInfo info;

  @Rule public ExpectedException thrown = none();

  private Failures failures;
  private Floats floats;

  @BeforeClass public static void setUpOnce() {
    info = new WritableAssertionInfo();
  }

  @Before public void setUp() {
    failures = spy(Failures.instance());
    floats = new Floats();
    floats.failures = failures;
  }

  @Test public void should_throw_error_if_actual_is_null() {
    thrown.expectAssertionError(unexpectedNull());
    floats.assertLessThanOrEqualTo(info, null, 8f);
  }

  @Test public void should_pass_if_actual_is_less_than_other() {
    floats.assertLessThanOrEqualTo(info, 6f, 8f);
  }

  @Test public void should_pass_if_actual_is_equal_to_other() {
    floats.assertLessThanOrEqualTo(info, 6f, 6f);
  }

  @Test public void should_fail_if_actual_is_greater_than_other() {
    try {
      floats.assertLessThanOrEqualTo(info, 8f, 6f);
      fail();
    } catch (AssertionError e) {}
    verify(failures).failure(info, isNotLessThanOrEqualTo(8f, 6f));
  }
}