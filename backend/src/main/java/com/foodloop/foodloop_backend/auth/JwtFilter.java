package com.foodloop.foodloop_backend.auth;

import com.foodloop.foodloop_backend.user.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    public JwtFilter(JwtService jwt, UserRepository users) { this.jwt = jwt; this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                String email = jwt.subject(h.substring(7));
                users.findByEmail(email).filter(u -> u.isActive()).ifPresent(u -> {
                    var auth = new UsernamePasswordAuthenticationToken(u, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (Exception ignored) { /* invalid or expired token -> stays anonymous */ }
        }
        chain.doFilter(req, res);
    }
}
