package com.proxio.backend.models.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/users/**",
                                "/api/auth/**"
                        )
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )

                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/locations/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
 

                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")

                        .successHandler((request, response, authentication) -> {
                        response.setStatus(200);
                        response.setContentType("application/json");
                        
                        SecurityUser userDetails = (SecurityUser) authentication.getPrincipal();
                        
                        String userJson = String.format(
                        "{\"email\":\"%s\", \"username\":\"%s\", \"role\":\"%s\"}",
                        userDetails.getEmail(),
                        userDetails.getUsername(),
                        userDetails.getRole());
                        
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