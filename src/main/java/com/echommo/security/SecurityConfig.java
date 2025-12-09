package com.echommo.security;

import com.echommo.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    // Cho phép mọi nguồn (trong môi trường dev), hoặc cụ thể http://localhost:5173
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép các request cấu hình (Pre-flight) đi qua
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. Các API công khai không cần đăng nhập
                        .requestMatchers("/api/auth/**", "/api/test/**", "/api/report/**").permitAll()

                        // 3. Tài nguyên tĩnh (Hình ảnh, CSS, JS)
                        .requestMatchers("/images/**", "/assets/**", "/*.html", "/*.js", "/*.css").permitAll()

                        // 4. [QUAN TRỌNG] Cấp quyền cho các tính năng game (Cần đăng nhập)
                        // Nếu không có đoạn này, mặc định sẽ rơi vào anyRequest().authenticated()
                        // Nhưng khai báo rõ ràng giúp dễ debug và tránh xung đột
                        .requestMatchers("/api/game/**").authenticated()
                        .requestMatchers("/api/battle/**").authenticated()
                        .requestMatchers("/api/market/**").authenticated()      // Mua bán
                        .requestMatchers("/api/inventory/**").authenticated()   // Hành trang
                        .requestMatchers("/api/leaderboard/**").authenticated() // Xếp hạng
                        .requestMatchers("/api/quest/**").authenticated()       // Nhiệm vụ
                        .requestMatchers("/api/friend/**").authenticated()      // Bạn bè
                        .requestMatchers("/api/chat/**").authenticated()        // Chat
                        .requestMatchers("/api/notification/**").authenticated()// Thông báo
                        .requestMatchers("/api/user/**").authenticated()        // Thông tin user

                        // 5. Tất cả các request còn lại bắt buộc phải đăng nhập
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}