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
package juzu.impl.request;

import juzu.Response;
import juzu.impl.common.AbstractAnnotatedElement;
import juzu.impl.common.Spliterator;
import juzu.impl.common.Tools;
import juzu.impl.inject.spi.InjectionContext;
import juzu.impl.value.ValueType;
import juzu.request.ApplicationContext;
import juzu.request.ClientContext;
import juzu.request.HttpContext;
import juzu.request.Phase;
import juzu.request.RequestContext;
import juzu.request.RequestParameter;
import juzu.request.Result;
import juzu.request.SecurityContext;
import juzu.request.UserContext;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A stage in the request pipeline.
 *
 * @author Julien Viet
 */
public abstract class Stage {

  /** . */
  private int index = 0;

  /** . */
  final Request request;

  /** . */
  final List<RequestFilter<?>> filters;

  public Request getRequest() {
    return request;
  }

  public Stage(Request request) {

    // Build the filter list
    List<RequestFilter<?>> filters = new ArrayList<RequestFilter<?>>();
    for (RequestFilter<?> filter : request.controllerPlugin.getInjectionContext().resolveInstances(RequestFilter.class)) {
      if (getClass().isAssignableFrom(filter.getStageType())) {
        filters.add(filter);
      }
    }

    //
    this.request = request;
    this.filters = filters;
  }

  public Result invoke() {
    if (index >= 0 && index < filters.size()) {
      RequestFilter plugin = filters.get(index);
      try {
        index++;
        return (Result)plugin.handle(this);
      }
      finally {
        index--;
      }
    }
    else if (index == filters.size()) {
      return result();
    }
    else {
      throw new AssertionError();
    }
  }

  protected abstract Result result();

  /**
   * This stage attempt to unmarshall the http entity when there is one.
   */
  public static class Unmarshalling extends Stage {

    public Unmarshalling(Request request) {
      super(request);
    }

    @Override
    protected Result result() {

      //
      Map<String, RequestParameter> parameterArguments = request.getParameterArguments();
      parameterArguments.putAll(request.bridge.getRequestArguments());

      //
      Map<ContextualParameter,Object> contextualArguments = request.getContextualArguments();
      for (ControlParameter controlParameter : request.handler.getParameters()) {
        if (controlParameter instanceof ContextualParameter) {
          ContextualParameter contextualParameter = (ContextualParameter)controlParameter;
          if (!contextualArguments.containsKey(contextualParameter)) {
            contextualArguments.put(contextualParameter, null);
          }
        }
      }
      contextualArguments.putAll(request.bridge.getContextualArguments(contextualArguments.keySet()));

      //
      ClientContext clientContext = request.bridge.getClientContext();
      if (clientContext != null) {
        String contentType = clientContext.getContentType();
        if (contentType != null) {
          Spliterator i = new Spliterator(contentType, ';');

          //
          String mediaType;
          if (i.hasNext()) {
            mediaType = i.next().trim();

            //
            if (!mediaType.equals("application/x-www-form-urlencoded")) {
              for (EntityUnmarshaller reader : Tools.loadService(EntityUnmarshaller.class, request.controllerPlugin.getApplication().getClassLoader())) {
                try {
                  if (reader.accept(mediaType)) {
                    reader.unmarshall(mediaType, clientContext, contextualArguments.entrySet(), parameterArguments);
                    break;
                  }
                }
                catch (IOException e) {
                  throw new UnsupportedOperationException("handle me gracefully", e);
                }
              }
            }
          }
        }
      }

      //
      return new Handler(request).invoke();
    }
  }

  /**
   * This stage prepares the handler for invocation, it determines the handler arguments and obtain the controller
   * object from the injection context of the application.
   */
  public static class Handler extends Stage {

    public Handler(Request request) {
      super(request);
    }

    public Result result() {

      ControllerHandler<?> handler = request.getHandler();
      InjectionContext<?, ?> manager = request.controllerPlugin.getInjectionContext();
      //
      Class<?> controllerType = handler.getType();
      request.controllerLifeCycle = manager.get(controllerType);
      if (request.controllerLifeCycle != null) {

        // Create context
        RequestContext context = new RequestContext(request, handler);

        // Build arguments
        Object[] args = new Object[handler.getParameters().size()];
        final Annotation[][] annotations = handler.getMethod().getParameterAnnotations();
        for (int i = 0;i < args.length;i++) {
          ControlParameter parameter = handler.getParameters().get(i);
          Object value;
          if (parameter instanceof PhaseParameter) {
            PhaseParameter phaseParam = (PhaseParameter)parameter;
            RequestParameter requestParam = request.getParameterArguments().get(phaseParam.getMappedName());
            if (requestParam != null) {
              ValueType<?> valueType = request.controllerPlugin.resolveValueType(phaseParam.getValueType());
              if (valueType != null) {
                List values = new ArrayList(requestParam.size());
                for (String s : requestParam) {
                  Object converted;
                  final int index = i;
                  try {
                    AbstractAnnotatedElement annotated = new AbstractAnnotatedElement() {
                      @Override
                      public Annotation[] getDeclaredAnnotations() {
                        return annotations[index];
                      }
                    };
                    converted = valueType.parse(annotated, s);
                  }
                  catch (Exception e) {
                    return Response.error(e).result();
                  }
                  values.add(converted);
                }
                value = phaseParam.getValue(values);
              } else {
                value = null;
              }
            } else {
              value = null;
            }
            Class<?> type = phaseParam.getType();
            if (value == null && type.isPrimitive()) {
              if (type == int.class) {
                value = 0;
              } else if (type == long.class) {
                value = 0L;
              } else if (type == byte.class) {
                value = (byte)0;
              } else if (type == short.class) {
                value = (short)0;
              } else if (type == boolean.class) {
                value = false;
              } else if (type == float.class) {
                value = 0.0f;
              } else if (type == double.class) {
                value = 0.0d;
              } else if (type == char.class) {
                value = '\u0000';
              }
            }
          } else if (parameter instanceof BeanParameter) {
            BeanParameter beanParam = (BeanParameter)parameter;
            Class<?> type = beanParam.getType();
            try {
              value = beanParam.createMappedBean(request.controllerPlugin, handler.requiresPrefix, type, beanParam.getName(), request.getParameterArguments());
            }
            catch (Exception e) {
              value = null;
            }
          } else {
            ContextualParameter contextualParameter = (ContextualParameter)parameter;
            value = request.getContextualArguments().get(contextualParameter);
            if (value == null) {
              Class<?> contextualType = contextualParameter.getType();
              if (RequestContext.class.isAssignableFrom(contextualType)) {
                value = context;
              } else if (HttpContext.class.isAssignableFrom(contextualType)) {
                value = request.getHttpContext();
              } else if (SecurityContext.class.isAssignableFrom(contextualType)) {
                value = request.getSecurityContext();
              } else if (ApplicationContext.class.isAssignableFrom(contextualType)) {
                value = request.getApplicationContext();
              } else if (UserContext.class.isAssignableFrom(contextualType)) {
                value = request.getUserContext();
              } else if (ClientContext.class.isAssignableFrom(contextualType) && (request.bridge.getPhase() == Phase.RESOURCE || request.bridge.getPhase() == Phase.ACTION)) {
                value = request.getClientContext();
              }
            }
          }
          args[i] = value;
        }

        // Get controller
        Object controller;
        try {
          controller = request.controllerLifeCycle.get();
        }
        catch (InvocationTargetException e) {
          return Response.error(e.getCause()).result();
        }

        //
        Stage.LifeCycle lifeCycle = new LifeCycle(request, context, controller, args);

        //
        return lifeCycle.invoke();
      } else {
        // Handle that...
        return null;
      }
    }
  }

  /**
   * This stage takes care of triggering the controller life cycle when the controller implements the
   * {@link juzu.request.RequestLifeCycle} interface.
   */
  public static class LifeCycle extends Stage {

    /** . */
    private final RequestContext context;

    /** . */
    private final Object controller;

    /** . */
    private final Object[] args;

    public LifeCycle(Request request, RequestContext context, Object controller, Object[] args) {
      super(request);
      this.context = context;
      this.controller = controller;
      this.args = args;
    }

    @Override
    protected Result result() {

      // Begin request callback
      if (controller instanceof juzu.request.RequestLifeCycle) {
        try {
          ((juzu.request.RequestLifeCycle)controller).beginRequest(context);
        }
        catch (Exception e) {
          return new Response.Error(e).result();
        }
      }

      //
      Response response = context.getResponse();
      Result result;
      if (response == null) {
        Stage.Invoke invokeStage = new Invoke(request, context, controller, args);
        result = invokeStage.invoke();

        // End request callback
        if (controller instanceof juzu.request.RequestLifeCycle) {
          try {
            ((juzu.request.RequestLifeCycle)controller).endRequest(context);
          }
          catch (Exception e) {
            context.setResponse(Response.error(e));
          }
        }

        //
        response = context.getResponse();
        if (response != null) {
          result = response.result();
        }
      } else {
        result = response.result();
      }

      //
      return result;
    }
  }

  /**
   * This stage invokes the handler on the controller with the specified arguments.
   */
  public static class Invoke extends Stage {

    /** . */
    private final RequestContext context;

    /** . */
    private final Object controller;

    /** . */
    private final Object[] args;

    public Invoke(Request request, RequestContext context, Object controller, Object[] args) {
      super(request);

      //
      this.controller = controller;
      this.context = context;
      this.args = args;
    }

    @Override
    protected Result result() {
      try {
        Object ret = context.getHandler().getMethod().invoke(controller, args);
        if (ret instanceof Response) {
          // We should check that it matches....
          // btw we should try to enforce matching during compilation phase
          // @Action -> Response.Action
          // @View -> Response.Mime
          // as we can do it
          return ((Response)ret).result();
        } else {
          return null;
        }
      }
      catch (InvocationTargetException e) {
        return Response.error(e.getCause()).result();
      }
      catch (IllegalAccessException e) {
        throw new UnsupportedOperationException("hanle me gracefully", e);
      }
    }
  }
}
