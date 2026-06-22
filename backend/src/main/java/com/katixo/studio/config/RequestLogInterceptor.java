package com.katixo.studio.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * One log line per internal-API request: method, path, response status and how long it took.
 * A lightweight access log so you can trace <em>what was called and whether it succeeded</em>
 * without a full logging framework. Registered for {@code /api/**} in {@link WebConfig}.
 *
 * <p>Logged under the {@code katixo.http} logger at INFO, e.g.:
 * <pre>POST /api/v1/copilot/chat -&gt; 200 (1183 ms)</pre>
 */
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("katixo.http");
    private static final String START_ATTR = "katixo.startNanos";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        long ms = (start instanceof Long startNanos) ? (System.nanoTime() - startNanos) / 1_000_000 : -1;
        String query = request.getQueryString();
        log.info("{} {}{} -> {} ({} ms){}",
                request.getMethod(),
                request.getRequestURI(),
                query == null ? "" : "?" + query,
                response.getStatus(),
                ms,
                ex == null ? "" : " EX=" + ex.getClass().getSimpleName());
    }
}
