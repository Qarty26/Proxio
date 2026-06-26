package com.proxio.backend.models.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, InternalTokenFilter internalTokenFilter) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/auth/**"
                        )
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )

                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(internalTokenFilter, BasicAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/stocks/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/stocks/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/stocks/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/stocks/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/vendors/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vendors/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vendors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/customers/me").authenticated()
                        .requestMatchers("/api/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/locations/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/locations/**").hasAnyRole("VENDOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/locations/**").hasAnyRole("VENDOR", "ADMIN")
                        .anyRequest().authenticated()
                )
 

                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")

                        .successHandler((request, response, authentication) -> {
                        response.setStatus(200);
                        response.setContentType("application/json");
                        
                        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
                        
                        String userJson = String.format(
                        "{\"email\":\"%s\", \"username\":\"%s\", \"role\":\"%s\",\"id\":\"%s\"}",
                        userDetails.getEmail(),
                        userDetails.getUsername(),
                        userDetails.getRole(),
                        userDetails.getId());
                        
                        response.getWriter().write(userJson);

                        })

                        .failureHandler((request, response, exception) -> {
                                exception.printStackTrace();
                                response.setStatus(401);
                        })

                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-key")
                        .permitAll()
                )

                .rememberMe(remember -> remember
                        .key("remember-key")
                        .tokenValiditySeconds(7 * 86400)
                );

        return http.build();
    }

    @Bean
    public InternalTokenFilter internalTokenFilter(
            @Value("${proxio.internal-auth.enabled:false}") boolean enabled,
            @Value("${proxio.internal-auth.secret:proxio-dev-internal-secret}") String secret) {
        return new InternalTokenFilter(enabled, secret);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
