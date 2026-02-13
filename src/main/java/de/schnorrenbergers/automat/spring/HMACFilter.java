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

        String keyId = request.getHeader("X-API-KEY-ID");
        String timestamp = request.getHeader("X-TIMESTAMP");
        String nonce = request.getHeader("X-NONCE");
        String signature = request.getHeader("X-SIGNATURE");

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

            if (hmacService.verifySignature(keyId, timestamp, nonce, signature)) {
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

        filterChain.doFilter(request, response);
    }
}
