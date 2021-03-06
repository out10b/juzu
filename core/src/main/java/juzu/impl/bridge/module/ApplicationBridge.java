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
package juzu.impl.bridge.module;

import juzu.impl.bridge.Bridge;
import juzu.impl.bridge.BridgeConfig;
import juzu.impl.bridge.BridgeContext;
import juzu.impl.common.Completion;
import juzu.impl.plugin.application.Application;
import juzu.impl.asset.AssetServer;
import juzu.impl.common.Logger;
import juzu.impl.common.RunMode;
import juzu.impl.common.Tools;
import juzu.impl.inject.spi.Injector;
import juzu.impl.resource.ResourceResolver;
import juzu.impl.runtime.ApplicationRuntime;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridge an application.
 *
 * @author Julien Viet
 */
public class ApplicationBridge extends Bridge {

  /** . */
  private final ModuleContextImpl module;

  /** . */
  private final AtomicReference<ApplicationRuntime<?, ?>> application;

  /** . */
  private final Injector injector;

  /** . */
  private final Logger log;

  public ApplicationBridge(
      ModuleContextImpl moduleContext,
      BridgeContext context,
      BridgeConfig config,
      AssetServer server,
      ResourceResolver resolver,
      Injector injector) {
    super(context, config, server, resolver);

    //
    this.module = moduleContext;
    this.log = context.getLogger(ApplicationBridge.class.getName());
    this.injector = injector;
    this.application = new AtomicReference<ApplicationRuntime<?, ?>>();
  }

  public RunMode getRunMode() {
    return module.getRunMode();
  }

  public Completion<Boolean> refresh(boolean recompile) {
    Completion<Boolean> refresh = module.runtime.refresh(recompile);
    if (refresh.isFailed()) {
      return refresh;
    } else {
      while (application.get() == null) {
        application.compareAndSet(null, new ApplicationRuntime(
            log,
            module.getRunMode(),
            module.runtime,
            injector,
            config.name,
            server,
            resolver));
      }
      return application.get().refresh();
    }
  }

  public Application getApplication() {
    return application.get().getApplication();
  }

  public void close() {
    ApplicationRuntime<?, ?> runtime = application.getAndSet(null);
    if (runtime != null) {
      Tools.safeClose(runtime);
    }
  }
}
