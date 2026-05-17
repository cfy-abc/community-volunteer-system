package com.volunteer.config;

import com.volunteer.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. OPTIONS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 2. Public auth endpoints
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/ping").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/wechat/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/wechat/auth-url").permitAll()
                        // 3. Public GET — activity list / detail / comments are publicly readable
                        .requestMatchers(HttpMethod.GET, "/api/activities").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/activities/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/activities/*/comments").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/activities/comments/*/replies").permitAll()
                        // 4. Authenticated GET — sign-status, applicants, sign-approvals
                        .requestMatchers(HttpMethod.GET, "/api/activities/*/sign-status").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/activities/*/sign-approvals").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/activities/*/applicants", "/api/activities/*/applicants/export").authenticated()
                        // 5. Authenticated POST/PUT/DELETE — activity operations
                        .requestMatchers(HttpMethod.POST, "/api/activities/*/apply").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/activities/*/checkin", "/api/activities/*/checkout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/activities/*/organizer-checkin").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/activities/*/comments", "/api/activities/comments/*/reply").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/activities/sign-approvals/*/approve", "/api/activities/sign-approvals/*/reject").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/activities/comments/*").authenticated()
                        // 6. Public GET — organizations
                        .requestMatchers(HttpMethod.GET, "/api/organizations/**").permitAll()
                        // 7. Uploads publicly accessible
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/upload", "/api/upload/batch").permitAll()
                        .requestMatchers("/upload", "/upload/batch").permitAll()
                        // 8. Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 9. Admin endpoints require ADMIN role
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 10. All other requests need authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有本地开发端口
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        // 允许所有 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 暴露所有响应头(包括 Content-Disposition)
        configuration.setExposedHeaders(Arrays.asList("*"));
        // 允许携带凭证(cookies)
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
