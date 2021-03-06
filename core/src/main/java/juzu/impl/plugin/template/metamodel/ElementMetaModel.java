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

package juzu.impl.plugin.template.metamodel;

import juzu.impl.common.JSON;
import juzu.impl.common.Path;
import juzu.impl.compiler.ElementHandle;

/**
 * A program element referencing a template.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ElementMetaModel extends TemplateRefMetaModel {

  /** . */
  final ElementHandle.Field element;

  /** The path declaration. */
  final Path.Absolute path;

  ElementMetaModel(ElementHandle.Field element, Path.Absolute path) {
    this.element = element;
    this.path = path;
  }

  @Override
  public void add(TemplateMetaModel template) {
    addChild(TemplateMetaModel.KEY, template);
  }

  public ElementHandle.Field getElement() {
    return element;
  }

  public Path.Absolute getPath() {
    return path;
  }

  public JSON toJSON() {
    JSON json = new JSON();
    json.set("element", element);
    json.set("template", getChild(TemplateMetaModel.KEY));
    return json;
  }
}
