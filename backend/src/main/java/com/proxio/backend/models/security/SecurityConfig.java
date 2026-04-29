package com.proxio.backend.models.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))

//                TODO: fill permissions once backend is complete.
//                 Here are just some examples that should be redone correctly

                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/api/users/**").permitAll()
                                .requestMatchers("/api/locations/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

            //TODO: These can be uncommented once those enpoints and pages exist.
            // For now they get stuck in a loop since they do not exist

//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .defaultSuccessUrl("/home", true)
//                        .permitAll()
//                )
//
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/login?logout")
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID")
//                        .permitAll()
//                )
//
//                .rememberMe(remember -> remember
//                        .key("remember-key")
//                        .tokenValiditySeconds(7 * 86400)
//                );

        return http.build();
    }

}
