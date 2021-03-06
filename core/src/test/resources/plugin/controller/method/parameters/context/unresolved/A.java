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

package plugin.controller.method.parameters.context.unresolved;

import juzu.Response;
import juzu.View;
import juzu.impl.plugin.controller.HandlerParametersTestCase;

/** @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a> */
public class A {

  @View(id = "index")
  public Response.Content index(Object unresolved) {
    if (unresolved == null) {
      HandlerParametersTestCase.WAS_NULL = true;
    }
    return Response.ok("" + A_.index());
  }
}
