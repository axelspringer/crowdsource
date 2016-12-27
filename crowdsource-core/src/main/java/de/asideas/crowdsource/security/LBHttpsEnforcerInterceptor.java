package de.asideas.crowdsource.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Ensures https by redirecting http requests to https://{@link #applicationUrl}
 */
@Component
@ConditionalOnProperty(name = "de.asideas.crowdsource.enforce-https", matchIfMissing = true)
public class LBHttpsEnforcerInterceptor extends HandlerInterceptorAdapter {

    public static final String X_FORWARDED_PROTO_HEADER = "X-FORWARDED-PROTO";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LBHttpsEnforcerInterceptor.class);

    @Value("${de.asideas.crowdsource.baseUrl:http://localhost:8080}")
    private String applicationUrl;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String forwardedHeader = request.getHeader(X_FORWARDED_PROTO_HEADER);
        if (StringUtils.isBlank(forwardedHeader) || !"HTTPS".equalsIgnoreCase(forwardedHeader)) {
            LOG.debug("redirecting non-https request with header: {}", forwardedHeader);
            response.sendRedirect(applicationUrl);
            return false;
        }
        return true;
    }
}
