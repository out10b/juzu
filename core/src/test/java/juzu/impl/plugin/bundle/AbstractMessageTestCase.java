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

package juzu.impl.plugin.bundle;

import juzu.impl.common.Tools;
import juzu.test.AbstractWebTestCase;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class AbstractMessageTestCase extends AbstractWebTestCase {

  @Test
  public void testInjection() throws Exception {
    URL url = applicationURL();
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestProperty("Accept-Language", "fr-FR");
    conn.connect();
    assertEquals(200, conn.getResponseCode());
    String result = Tools.read(conn.getInputStream());
    assertTrue("Was expecting <" + result + "> to contain def", result.contains("def"));
  }
}
