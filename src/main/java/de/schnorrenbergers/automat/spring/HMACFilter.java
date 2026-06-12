package de.schnorrenbergers.automat.spring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;

@Component
public class HMACFilter extends OncePerRequestFilter {

    private static final long MAX_SKEW_SECONDS = 30;

    @Autowired
    private HmacService hmacService;

    @Autowired
    private NonceStore nonceStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        // Wrap the request so the body can be read multiple times (filter + controller)
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

        String keyId = wrappedRequest.getHeader("X-API-KEY-ID");
        String timestamp = wrappedRequest.getHeader("X-TIMESTAMP");
        String nonce = wrappedRequest.getHeader("X-NONCE");
        String signature = wrappedRequest.getHeader("X-SIGNATURE");

        if (keyId == null || timestamp == null || nonce == null || signature == null) {
            sendAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing HMAC headers");
            return;
        }

        try {
            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();

            if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
                sendAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "Timestamp out of range");
                return;
            }

            if (nonceStore.isReplay(keyId, nonce)) {
                sendAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "Replay attack detected");
                return;
            }

            // Read the cached body without consuming it for downstream
            byte[] bodyBytes = wrappedRequest.getCachedBody();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            if (hmacService.verifySignature(keyId, timestamp, nonce, signature, body)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(keyId, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                sendAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }
        } catch (NumberFormatException e) {
            sendAuthError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * Writes the status and message directly to the response instead of calling
     * {@link HttpServletResponse#sendError}. {@code sendError} triggers Spring Boot's
     * {@code /error} dispatch, which re-enters this same security filter chain; since that
     * request is anonymous and {@code anyRequest().authenticated()} rejects it, the original
     * status and message get overwritten with a bare 403 from {@code Http403ForbiddenEntryPoint},
     * hiding the actual rejection reason from the caller.
     */
    private void sendAuthError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
