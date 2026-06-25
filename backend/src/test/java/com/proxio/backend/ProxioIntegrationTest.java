package com.proxio.backend;

import com.proxio.backend.models.Customer;
import com.proxio.backend.models.User;
import com.proxio.backend.models.enums.Role;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.OrderItemRepository;
import com.proxio.backend.repositories.OrderRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.UserRatingRepository;
import com.proxio.backend.repositories.WeeklyOfferRepository;
import com.proxio.backend.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProxioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WeeklyOfferRepository weeklyOfferRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRatingRepository userRatingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRatingRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        for (String email : java.util.List.of("local.user@proxio.test", "login.user@proxio.test", "normal.user@proxio.test", "buyer@proxio.test")) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
    }

    @Test
    void signupCreatesUserWithEncodedPassword() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("fullName", "Local User")
                        .param("email", "local.user@proxio.test")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User user = userRepository.findByEmail("local.user@proxio.test").orElseThrow();
        assertTrue(passwordEncoder.matches("secret123", user.getPassword()));
        assertTrue(user.getRole() == Role.USER);
        assertTrue(customerRepository.findByUserEmail("local.user@proxio.test").isPresent());
    }

    @Test
    void formLoginRedirectsAuthenticatedUserToMarket() throws Exception {
        createUser("login.user@proxio.test", "secret123", Role.USER);

        mockMvc.perform(post("/login")
                        .with(csrf())
                .param("username", "login.user@proxio.test")
                .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/market"));
    }

    @Test
    void userRoleCannotAccessAdminUsersApi() throws Exception {
        createUser("normal.user@proxio.test", "secret123", Role.USER);

        mockMvc.perform(get("/api/users")
                        .with(httpBasic("normal.user@proxio.test", "secret123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanPlaceOrderFromOffer() throws Exception {
        User buyer = createUser("buyer@proxio.test", "secret123", Role.USER);
        Customer customer = new Customer();
        customer.setUser(buyer);
        customerRepository.save(customer);
        Long offerId = weeklyOfferRepository.findAll().get(0).getId();

        mockMvc.perform(post("/offers/" + offerId + "/order")
                        .with(csrf())
                        .with(user("buyer@proxio.test").roles("USER"))
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));
    }

    private User createUser(String email, String password, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName("Test User");
        user.setRole(role);
        return userService.create(user);
    }
}
