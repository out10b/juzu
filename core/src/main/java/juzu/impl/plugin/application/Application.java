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

package juzu.impl.plugin.application;

import juzu.Scope;
import juzu.Handler;
import juzu.impl.common.JSON;
import juzu.impl.common.Tools;
import juzu.impl.inject.BeanDescriptor;
import juzu.impl.inject.spi.Injector;
import juzu.impl.plugin.Service;
import juzu.impl.plugin.ServiceContext;
import juzu.impl.plugin.ServiceDescriptor;
import juzu.impl.plugin.application.descriptor.ApplicationDescriptor;
import juzu.impl.inject.spi.InjectionContext;
import juzu.impl.resource.ResourceResolver;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@Singleton
public class Application implements ResourceResolver {

  /** . */
  private final ApplicationDescriptor descriptor;

  /** . */
  InjectionContext<?, ?> injectionContext;

  /** . */
  final ResourceResolver resourceResolver;

  /** . */
  final Injector injector;

  /** . */
  private Map<String, ApplicationService> plugins;

  /** . */
  private final ClassLoader classLoader;

  public Application(Injector injector, ApplicationDescriptor descriptor, ResourceResolver resourceResolver) {
    this.classLoader = descriptor.getApplicationLoader();
    this.injectionContext = null;
    this.descriptor = descriptor;
    this.injector = injector;
    this.resourceResolver = resourceResolver;
    this.plugins = Collections.emptyMap();
  }

  public void start() throws Exception {

    final ResourceResolver applicationResolver = new ResourceResolver() {
      public URL resolve(String uri) {
        if (uri == null) {
          throw new NullPointerException("No null URI accepted");
        }
        if (uri.startsWith("/")) {
          return classLoader.getResource(uri.substring(1));
        } else {
          return null;
        }
      }
    };

    // Take care of plugins
    HashMap<String, ApplicationService> plugins = new HashMap<String, ApplicationService>();
    for (ApplicationService plugin : ServiceLoader.load(ApplicationService.class)) {
      plugins.put(plugin.getName(), plugin);
    }
    HashSet<String> names = new HashSet<String>(descriptor.getConfig().names());
    HashMap<ApplicationService, JSON> configs = new HashMap<ApplicationService, JSON>();
    for (ApplicationService plugin : plugins.values()) {
      String name = plugin.getName();
      if (names.remove(name)) {
        configs.put(plugin, descriptor.getConfig().getJSON(plugin.getName()));
      } else {
        configs.put(plugin, null);
      }
    }
    if (names.size() > 0) {
      throw new UnsupportedOperationException("Handle me gracefully : missing plugins " + names);
    }

    //
    HashMap<String, ServiceDescriptor> pluginDescriptors = new HashMap<String, ServiceDescriptor>();
    for (final Map.Entry<ApplicationService, JSON> entry : configs.entrySet()) {
      ApplicationService plugin = entry.getKey();
      ServiceContext pluginContext = new ServiceContext() {
        public JSON getConfig() {
          return entry.getValue();
        }
        public ClassLoader getClassLoader() {
          return classLoader;
        }
        public ResourceResolver getServerResolver() {
          return resourceResolver;
        }
        public ResourceResolver getApplicationResolver() {
          return applicationResolver;
        }
      };
      plugin.setApplication(descriptor);
      ServiceDescriptor pluginDescriptor = plugin.init(pluginContext);
      if (pluginDescriptor != null) {
        pluginDescriptors.put(plugin.getName(), pluginDescriptor);
      }
    }

    //
    for (Iterator<String> i = plugins.keySet().iterator();i.hasNext();) {
      String name = i.next();
      if (!pluginDescriptors.containsKey(name)) {
        i.remove();
      }
    }

    // Bind the plugins
    for (Service service : plugins.values()) {

      // Bind the plugin as a bean
      Class aClass = service.getClass();
      Object o = service;
      injector.bindBean(aClass, null, o);
    }

    // Bind the beans
    for (ServiceDescriptor pluginDescriptor : pluginDescriptors.values()) {
      for (BeanDescriptor bean : pluginDescriptor.getBeans()) {
        bean.bind(injector);
      }
    }

    // Bind the application descriptor
    injector.bindBean(ApplicationDescriptor.class, null, descriptor);

    // Bind ourself
    injector.bindBean(Application.class, null, this);

    // Bind the scopes
    for (Scope scope : Scope.values()) {
      injector.addScope(scope);
    }

    // Filter the classes:
    // any class beginning with juzu. is refused
    // any class prefixed with the application package is accepted
    // any other application class is refused (i.e a class having an ancestor package annotated with @Application)
    Handler<Class<?>, Boolean> filter = new Handler<Class<?>, Boolean>() {
      HashSet<String> blackList = new HashSet<String>();
      public Boolean handle(Class<?> argument) {
        if (argument.getName().startsWith("juzu.")) {
          return false;
        } else if (argument.getPackage().getName().startsWith(descriptor.getPackageName())) {
          return true;
        } else {
          for (String currentPkg = argument.getPackage().getName();currentPkg != null;currentPkg = Tools.parentPackageOf(currentPkg)) {
            if (blackList.contains(currentPkg)) {
              return false;
            } else {
              try {
                Class<?> packageClass = classLoader.loadClass(currentPkg + ".package-info");
                juzu.Application ann = packageClass.getAnnotation(juzu.Application.class);
                if (ann != null) {
                  blackList.add(currentPkg);
                  return false;
                }
              }
              catch (ClassNotFoundException e) {
                // Skip it
              }
            }
          }
          return true;
        }
      }
    };

    //
    try {
      this.injectionContext = injector.create(filter);
      this.plugins = plugins;
    }
    catch (Exception e) {
      throw new UnsupportedOperationException("handle me gracefully", e);
    }
  }

  public String getName() {
    return descriptor.getName();
  }

  public ClassLoader getClassLoader() {
    return injectionContext.getClassLoader();
  }

  public InjectionContext<?, ?> getInjectionContext() {
    return injectionContext;
  }

  public <T> T resolveBean(Class<T> beanType) {
    return injectionContext.resolveInstance(beanType);
  }

  public <T> Iterable<T> resolveBeans(final Class<T> beanType) {
    return injectionContext.resolveInstances(beanType);
  }

  public ApplicationService getPlugin(String pluginName) {
    return plugins.get(pluginName);
  }

  public ApplicationDescriptor getDescriptor() {
    return descriptor;
  }

  public Object resolveBean(String name) throws InvocationTargetException {
    return resolveBean(injectionContext, name);
  }

  public URL resolve(String uri) {
    return classLoader.getResource(uri.substring(1));
  }

  private <B, I> Object resolveBean(InjectionContext<B, I> manager, String name) throws InvocationTargetException {
    B bean = manager.resolveBean(name);
    if (bean != null) {
      I cc = manager.createContext(bean);
      return manager.getInstance(bean, cc);
    }
    else {
      return null;
    }
  }
}
