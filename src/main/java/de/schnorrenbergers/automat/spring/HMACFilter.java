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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        //TODO: add important security functions
        return path.equals("/api/pineg");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        // Wrap the request so the body can be read multiple times (filter + controller)
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

        String keyId = wrappedRequest.getHeader("X-API-KEY-ID");
        String timestamp = wrappedRequest.getHeader("X-TIMESTAMP");
        String nonce = wrappedRequest.getHeader("X-NONCE");
        String signature = wrappedRequest.getHeader("X-SIGNATURE");

        if (keyId == null || timestamp == null || nonce == null || signature == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing HMAC headers");
            return;
        }

        try {
            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();

            if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Timestamp out of range");
                return;
            }

            if (nonceStore.isReplay(keyId, nonce)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Replay attack detected");
                return;
            }

            // Read the cached body without consuming it for downstream
            byte[] bodyBytes = wrappedRequest.getCachedBody();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            System.out.println("[DEBUG_LOG] Verifying signature for keyId: " + keyId);
            System.out.println("[DEBUG_LOG] Timestamp: " + timestamp);
            System.out.println("[DEBUG_LOG] Nonce: " + nonce);
            System.out.println("[DEBUG_LOG] Signature: " + signature);
            System.out.println("[DEBUG_LOG] Body: '" + body + "'");

            if (hmacService.verifySignature(keyId, timestamp, nonce, signature, body)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(keyId, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }
}
