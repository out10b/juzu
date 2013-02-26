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

package juzu.impl.plugin.controller;

import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.impl.plugin.controller.descriptor.ControllersDescriptor;
import juzu.impl.request.Method;
import juzu.impl.request.Parameter;
import juzu.impl.request.PhaseParameter;
import juzu.impl.common.Cardinality;
import juzu.request.Phase;
import juzu.test.AbstractTestCase;
import juzu.test.CompilerAssert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ActionMethodTestCase extends AbstractTestCase {

  @Override
  public void setUp() throws Exception {
    CompilerAssert<?, ?> compiler = compiler("plugin.controller.method.action");
    compiler.assertCompile();
    aClass = compiler.assertClass("plugin.controller.method.action.A");
    compiler.assertClass("plugin.controller.method.action.A_");

    //
    Class<?> appClass = compiler.assertClass("plugin.controller.method.action.Application");
    descriptor = ApplicationDescriptor.create(appClass);
    controllerDescriptor = new ControllersDescriptor(descriptor);
  }

  /** . */
  private Class<?> aClass;

  /** . */
  private ApplicationDescriptor descriptor;

  /** . */
  private ControllersDescriptor controllerDescriptor;

  @Test
  public void testNoArg() throws Exception {
    Method cm = controllerDescriptor.getMethod(aClass, "noArg");
    assertEquals("noArg", cm.getName());
    assertEquals(Phase.ACTION, cm.getPhase());
    assertEquals(Collections.<Parameter>emptyList(), cm.getParameters());
  }

  @Test
  public void testStringArg() throws Exception {
    Method cm = controllerDescriptor.getMethod(aClass, "oneArg", String.class);
    assertEquals("oneArg", cm.getName());
    assertEquals(Phase.ACTION, cm.getPhase());
    assertEquals(Arrays.asList(new PhaseParameter("foo", null, Cardinality.SINGLE)), cm.getParameters());
  }
}
