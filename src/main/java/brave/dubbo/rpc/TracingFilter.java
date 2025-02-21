/*
 * Copyright 2013-2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package brave.dubbo.rpc;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.spring.extension.SpringExtensionFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.springframework.util.StringUtils;

import brave.Span;
import brave.Span.Kind;
import brave.Tracer;
import brave.Tracing;
import brave.internal.Platform;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;

@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, value = "tracing")
// http://dubbo.apache.org/en-us/docs/dev/impls/filter.html
// public constructor permitted to allow dubbo to instantiate this
public final class TracingFilter implements Filter {

  Tracer tracer;
  TraceContext.Extractor<Invocation> extractor;
  TraceContext.Injector<Invocation> injector;
  volatile boolean inited = false;

  /**
   * {@link ExtensionLoader} supplies the tracing implementation which must be named "tracing". For
   * example, if using the {@link SpringExtensionFactory}, only a bean named "tracing" will be
   * injected.
   */
  public void setTracing(Tracing tracing) {
    tracer = tracing.tracer();
    extractor = tracing.propagation().extractor(GETTER);
    injector = tracing.propagation().injector(SETTER);
    tracing.currentTraceContext();
    inited = true;
  }

  @Override
  public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
    if (!inited) return invoker.invoke(invocation);

    RpcContext rpcContext = RpcContext.getContext();
    Kind kind = rpcContext.isProviderSide() ? Kind.SERVER : Kind.CLIENT;
    final Span span;
    if (kind.equals(Kind.CLIENT)) {
      span = tracer.nextSpan();
      injector.inject(span.context(), invocation);
    } else {
      TraceContextOrSamplingFlags extracted = extractor.extract(invocation);
      span = extracted.context() != null
          ? tracer.joinSpan(extracted.context())
          : tracer.nextSpan(extracted);
    }

    if (!span.isNoop()) {
      span.kind(kind);
      String service = invoker.getInterface().getSimpleName();
      String method = RpcUtils.getMethodName(invocation);
      span.name(service + "/" + method + ":" + invoker.getUrl().getParameter(CommonConstants.VERSION_KEY, ""));
      remoteEndpoint(rpcContext, span);
      span.start();
    }

    boolean isOneway = false, deferFinish = false;
    try (Tracer.SpanInScope scope = tracer.withSpanInScope(span)) {
      Result result = invoker.invoke(invocation);
      if (result.hasException()) {
        onError(result.getException(), span);
      }
      isOneway = RpcUtils.isOneway(invoker.getUrl(), invocation);
      Future<Object> future = rpcContext.getFuture(); // the case on async client invocation
      if (future instanceof FutureAdapter && !future.isDone()) {
        deferFinish = true;
        ((FutureAdapter) future).whenCompleteAsync(new FinishSpanCallback(span));
      }
      return result;
    } catch (Error | RuntimeException e) {
      onError(e, span);
      throw e;
    } finally {
      if (isOneway) {
        span.flush();
      } else if (!deferFinish) {
        span.finish();
      }
    }
  }

  private static void remoteEndpoint(RpcContext rpcContext, Span span) {
    InetSocketAddress remoteAddress = rpcContext.getRemoteAddress();
    if (remoteAddress != null) {
      span.remoteIpAndPort(Platform.get().getHostString(remoteAddress), remoteAddress.getPort());
    }

    String remoteApplicationName = rpcContext.getRemoteApplicationName();
    if (StringUtils.hasText(remoteApplicationName)) {
      span.remoteServiceName(remoteApplicationName);
    } else {
      span.remoteServiceName(rpcContext.getRemoteHostName());
    }
  }

  static void onError(Throwable error, Span span) {
    span.error(error);
    if (error instanceof RpcException) {
      span.tag("dubbo.error_code", Integer.toString(((RpcException) error).getCode()));
    }
  }

  static final Propagation.Getter<Invocation, String> GETTER =
      new Propagation.Getter<Invocation, String>() {
        @Override
        public String get(Invocation carrier, String key) {
          return carrier.getAttachment(key);
        }

        @Override
        public String toString() {
          return "Invocation::getAttachment";
        }
      };

  static final Propagation.Setter<Invocation, String> SETTER =
      new Propagation.Setter<Invocation, String>() {
        @Override
        public void put(Invocation carrier, String key, String value) {
          carrier.setAttachment(key, value);
        }

        @Override
        public String toString() {
          return "Invocation::setAttachment";
        }
      };


  static final class FinishSpanCallback implements BiConsumer<Object, Throwable> {
    final Span span;

    FinishSpanCallback(Span span) {
      this.span = span;
    }

    @Override
    public void accept(Object result, Throwable ex) {
      if (ex != null) {
        onError(ex, span);
      }
      span.finish();
    }
  }
}
