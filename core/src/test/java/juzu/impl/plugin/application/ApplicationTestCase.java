/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.plugin.application;

import juzu.impl.compiler.CompilationError;
import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.impl.plugin.controller.descriptor.ControllersDescriptor;
import juzu.impl.request.Method;
import juzu.test.AbstractTestCase;
import juzu.test.CompilerAssert;
import org.junit.Test;

import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ApplicationTestCase extends AbstractTestCase {

  @Test
  public void testDefaultController() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.application.default_controller");
    compiler.assertCompile();
    Class<?> appClass = compiler.assertClass("plugin.application.default_controller.Application");
    Class<?> aClass = compiler.assertClass("plugin.application.default_controller.A");

    //
    ApplicationDescriptor desc = ApplicationDescriptor.create(appClass);
    ControllersDescriptor controller = new ControllersDescriptor(desc);
    assertSame(aClass, controller.getDefault());
  }

  public void _testMethodId() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.application.method.id");
    compiler.assertCompile();
    Class<?> appClass = compiler.assertClass("plugin.application.method.id.Application");
    Class<?> aClass = compiler.assertClass("plugin.application.method.id.A");

    //
    ApplicationDescriptor desc = ApplicationDescriptor.create(appClass);
    ControllersDescriptor controllerDesc = new ControllersDescriptor(desc);
    Method a = controllerDesc.getMethod(aClass, "a");
    Method b = controllerDesc.getMethod(aClass, "b");
    Method c = controllerDesc.getMethod(aClass, "c");

    //
    assertEquals("foo", a.getId());
    assertEquals("bar", b.getId());
    assertEquals("juu", c.getId());

    //
    assertSame(a, controllerDesc.getMethodById("foo"));
    assertSame(b, controllerDesc.getMethodById("bar"));
    assertSame(c, controllerDesc.getMethodById("juu"));
  }

  public void _testDuplicateMethod() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.application.method.duplicate");
    List<CompilationError> errors = compiler.failCompile();
    assertEquals("Was expecting a single error instead of " + errors, 1, errors.size());
    assertEquals("/plugin/application/method/duplicate/A.java", errors.get(0).getSource());
  }

  public void _testPrefix() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.application.prefix");
    compiler.assertCompile();
  }

  @Test
  public void testMultiple() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.application.multiple");
    compiler.assertCompile();

    //
    Class<?> app1Class = compiler.assertClass("plugin.application.multiple.app1.Application");
    Class<?> a1Class = compiler.assertClass("plugin.application.multiple.app1.A");
    ApplicationDescriptor desc1 = ApplicationDescriptor.create(app1Class);
    ControllersDescriptor controllerDesc1 = new ControllersDescriptor(desc1);
    assertSame(a1Class, controllerDesc1.getControllers().get(0).getType());

    //
    Class<?> app2Class = compiler.assertClass("plugin.application.multiple.app2.Application");
    Class<?> a2Class = compiler.assertClass("plugin.application.multiple.app2.A");
    ApplicationDescriptor desc2 = ApplicationDescriptor.create(app2Class);
    ControllersDescriptor controllerDesc2 = new ControllersDescriptor(desc2);
    assertSame(a2Class, controllerDesc2.getControllers().get(0).getType());
  }
}
