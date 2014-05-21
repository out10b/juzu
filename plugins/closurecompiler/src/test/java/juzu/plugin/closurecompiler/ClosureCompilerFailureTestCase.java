/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.plugin.closurecompiler;

import juzu.impl.plugin.asset.MinifierFailureTestCase;
import juzu.test.AbstractTestCase;
import org.junit.Test;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ClosureCompilerFailureTestCase extends AbstractTestCase {

  @Test
  public void testMalformedScript() throws Exception {
    MinifierFailureTestCase.assertFail("plugin.closurecompiler.failure", "test-min.js", "Malformed asset:\n" +
        "  JSC_PARSE_ERROR. Parse error. syntax error at test-min.js line 1 : 2\n" +
        "  ");
  }
}