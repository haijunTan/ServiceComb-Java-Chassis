/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.serviceregistry.client.http;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.servicecomb.foundation.auth.AuthHeaderProvider;
import io.servicecomb.foundation.common.net.IpPort;
import io.servicecomb.foundation.vertx.client.http.HttpClientWithContext;
import io.servicecomb.serviceregistry.config.ServiceRegistryConfig;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;

final class RestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);

  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  private static final String HEADER_USER_AGENT = "User-Agent";

  private static final String HEADER_TENANT_NAME = "x-domain-name";

  private static final ServiceLoader<AuthHeaderProvider> authHeaderProviders =
      ServiceLoader.load(AuthHeaderProvider.class);

  private RestUtils() {
  }

  public static void httpDo(RequestContext requestContext, Handler<RestResponse> responseHandler) {
    HttpClientWithContext vertxHttpClient = HttpClientPool.INSTANCE.getClient();
    vertxHttpClient.runOnContext(httpClient -> {
      IpPort ipPort = requestContext.getIpPort();
      HttpMethod httpMethod = requestContext.getMethod();
      RequestParam requestParam = requestContext.getParams();

      if (ipPort == null) {
        LOGGER.error("request address is null");
        responseHandler.handle(new RestResponse(requestContext, null));
        return;
      }

      // query params
      StringBuilder url = new StringBuilder(requestContext.getUri());
      String queryParams = requestParam.getQueryParams();
      if (!queryParams.isEmpty()) {
        url.append(url.lastIndexOf("?") > 0 ? "&" : "?")
            .append(queryParams);
      }

      HttpClientRequest httpClientRequest = httpClient
          .request(httpMethod, ipPort.getPort(), ipPort.getHostOrIp(), url.toString(), response -> {
            responseHandler.handle(new RestResponse(requestContext, response));
          });

      httpClientRequest.setTimeout(ServiceRegistryConfig.INSTANCE.getRequestTimeout())
          .exceptionHandler(e -> {
            LOGGER.error("{} {} fail, endpoint is {}:{}, message: {}",
                httpMethod,
                url.toString(),
                ipPort.getHostOrIp(),
                ipPort.getPort(),
                e.getMessage());
            responseHandler.handle(new RestResponse(requestContext, null));
          });

      // headers
      addDefaultHeaders(httpClientRequest);

      if (requestParam.getHeaders() != null && requestParam.getHeaders().size() > 0) {
        for (Map.Entry<String, String> header : requestParam.getHeaders().entrySet()) {
          httpClientRequest.putHeader(header.getKey(), header.getValue());
        }
      }

      // cookies header
      if (requestParam.getCookies() != null && requestParam.getCookies().size() > 0) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> cookie : requestParam.getCookies().entrySet()) {
          stringBuilder.append(cookie.getKey())
              .append("=")
              .append(cookie.getValue())
              .append("; ");
        }
        httpClientRequest.putHeader("Cookie", stringBuilder.toString());
      }

      // body
      if (httpMethod != HttpMethod.GET && requestParam.getBody() != null && requestParam.getBody().length > 0) {
        httpClientRequest.end(Buffer.buffer(requestParam.getBody()));
      } else {
        httpClientRequest.end();
      }
    });
  }

  public static RequestContext createRequestContext(HttpMethod method, IpPort ipPort, String uri,
      RequestParam requestParam) {
    RequestContext requestContext = new RequestContext();
    requestContext.setMethod(method);
    requestContext.setIpPort(ipPort);
    requestContext.setUri(uri);
    requestContext.setParams(requestParam);
    return requestContext;
  }

  public static void addDefaultHeaders(HttpClientRequest request) {
    request.headers().addAll(getDefaultHeaders());
  }

  public static MultiMap getDefaultHeaders() {
    // add token header
    return new CaseInsensitiveHeaders().add(HEADER_CONTENT_TYPE, "application/json")
        .add(HEADER_USER_AGENT, "cse-serviceregistry-client/1.0.0")
        .add(HEADER_TENANT_NAME, ServiceRegistryConfig.INSTANCE.getTenantName())
        .addAll(authHeaders());
  }

  public static void get(IpPort ipPort, String uri, RequestParam requestParam,
      Handler<RestResponse> responseHandler) {
    httpDo(createRequestContext(HttpMethod.GET, ipPort, uri, requestParam), responseHandler);
  }

  public static void post(IpPort ipPort, String uri, RequestParam requestParam,
      Handler<RestResponse> responseHandler) {
    httpDo(createRequestContext(HttpMethod.POST, ipPort, uri, requestParam), responseHandler);
  }

  public static void put(IpPort ipPort, String uri, RequestParam requestParam,
      Handler<RestResponse> responseHandler) {
    httpDo(createRequestContext(HttpMethod.PUT, ipPort, uri, requestParam), responseHandler);
  }

  public static void delete(IpPort ipPort, String uri, RequestParam requestParam,
      Handler<RestResponse> responseHandler) {
    httpDo(createRequestContext(HttpMethod.DELETE, ipPort, uri, requestParam), responseHandler);
  }

  private static Map<String, String> authHeaders() {
    Map<String, String> headers = new HashMap<>();
    authHeaderProviders.forEach(provider -> headers.putAll(provider.authHeaders()));
    return headers;
  }
}
