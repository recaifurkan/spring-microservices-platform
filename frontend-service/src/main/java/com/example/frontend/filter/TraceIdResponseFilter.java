package com.example.frontend.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds an X-Trace-Id header to every HTTP response.
 *
 * HandlerInterceptor.preHandle() is used because:
 *  - Micrometer Tracing already created the span in the tracing filter (MDC is populated)
 *  - The response has not been committed yet (headers can still be added)
 *  - It works reliably on the first request as well
 *
 * Example:
 *   curl -I http://localhost:8085/home
 *   → X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
 */
@Component
public class TraceIdResponseFilter implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER  = "X-Span-Id";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String traceId = MDC.get("traceId");
        String spanId  = MDC.get("spanId");

        if (traceId != null && !traceId.isBlank()) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }
        if (spanId != null && !spanId.isBlank()) {
            response.setHeader(SPAN_ID_HEADER, spanId);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        // Re-check before view rendering — if it was missing in preHandle, fill it here
        if (response.getHeader(TRACE_ID_HEADER) == null) {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isBlank()) {
                response.setHeader(TRACE_ID_HEADER, traceId);
            }
        }
    }
}

