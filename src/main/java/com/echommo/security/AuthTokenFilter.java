package com.echommo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    // [FIX] Sử dụng Logger riêng của class này để kiểm soát log tốt hơn
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            // [DEBUG] Log nhẹ nếu cần check xem request có mang token không (Uncomment khi cần debug sâu)
            // if (jwt == null) {
            //     logger.debug("No JWT found in request to: {}", request.getRequestURI());
            // }

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // Load lại thông tin mới nhất từ DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // [FIX REAL-TIME BAN]
                // Kiểm tra nếu tài khoản bị vô hiệu hóa (Active = false) hoặc bị khóa (Locked)
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Trả về 403
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    // Xác định thông báo lỗi cụ thể
                    String errorType = !userDetails.isEnabled() ? "BANNED" : "LOCKED";
                    String message = !userDetails.isEnabled()
                            ? "Tài khoản của bạn đã bị phong ấn vĩnh viễn."
                            : "Tài khoản đang bị khóa (Captcha/Vi phạm).";

                    // Trả về JSON để Frontend bắt được
                    response.getWriter().write(String.format("{\"error\": \"%s\", \"message\": \"%s\"}", errorType, message));
                    response.flushBuffer(); // Đẩy response đi ngay lập tức
                    return; // Dừng filter chain, không cho request đi tiếp vào Controller
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        // Kiểm tra header có đúng định dạng Bearer Token không
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}