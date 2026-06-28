package com.proxio.backend.controllers;

import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.Product;
import com.proxio.backend.models.enums.Role;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.repositories.ProductRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void productsPagedEndpointReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/products/paged")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("content")))
                .andExpect(jsonPath("$", hasKey("totalElements")));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void vendorsPagedEndpointReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/vendors/paged")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "farmName")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("content")))
                .andExpect(jsonPath("$", hasKey("totalElements")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void customersPagedEndpointReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/customers/paged")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "id")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("content")))
                .andExpect(jsonPath("$", hasKey("totalElements")));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "vendor-create-product@example.com", roles = "VENDOR")
    void vendorCanCreateProduct() throws Exception {
        User user = new User();
        user.setEmail("vendor-create-product@example.com");
        user.setPassword("encoded-password");
        user.setFullName("Vendor Create Product");
        user.setRole(Role.VENDOR);
        User savedUser = userRepository.save(user);

        Vendor vendor = new Vendor();
        vendor.setUser(savedUser);
        vendor.setFarmName("Integration Farm");
        Vendor savedVendor = vendorRepository.save(vendor);

        String body = """
                {
                  "name": "Integration Apples",
                  "description": "Fresh apples created through integration test",
                  "unit": "kg",
                  "category": "FRUITS",
                  "vendor": { "id": %d }
                }
                """.formatted(savedVendor.getId());

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Apples"))
                .andExpect(jsonPath("$.vendor.id").value(savedVendor.getId()));
    }

    @Test
    @WithMockUser(username = "customer-cannot-create-product@example.com", roles = "CUSTOMER")
    void customerCannotCreateProduct() throws Exception {
        User vendorUser = user("customer-create-target-vendor@example.com", Role.VENDOR);
        Vendor vendor = vendorRepository.save(vendor("Customer Create Target Farm", vendorUser));

        String body = """
                {
                  "name": "Blocked Apples",
                  "description": "This product should not be created by a customer",
                  "unit": "kg",
                  "category": "FRUITS",
                  "vendor": { "id": %d }
                }
                """.formatted(vendor.getId());

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "second-vendor@example.com", roles = "VENDOR")
    void vendorCannotDeleteAnotherVendorsProduct() throws Exception {
        User ownerUser = user("owner-vendor@example.com", Role.VENDOR);
        Vendor ownerVendor = vendorRepository.save(vendor("Owner Farm", ownerUser));
        Product product = productRepository.save(product("Owner Apples", ownerVendor));

        User secondUser = user("second-vendor@example.com", Role.VENDOR);
        vendorRepository.save(vendor("Second Farm", secondUser));

        mockMvc.perform(delete("/api/products/{id}", product.getId()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void vendorCanCreateOwnProfileFromUserId() throws Exception {
        User user = new User();
        user.setEmail("vendor-profile-create@example.com");
        user.setPassword("encoded-password");
        user.setFullName("Vendor Profile Create");
        user.setRole(Role.VENDOR);
        User savedUser = userRepository.save(user);

        String body = """
                {
                  "user": { "id": %d },
                  "farmName": "Created From Frontend Farm",
                  "description": "Created during product flow"
                }
                """.formatted(savedUser.getId());

        mockMvc.perform(post("/api/vendors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmName").value("Created From Frontend Farm"))
                .andExpect(jsonPath("$.user.id").value(savedUser.getId()));
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setFullName(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    private Vendor vendor(String farmName, User user) {
        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setFarmName(farmName);
        return vendor;
    }

    private Product product(String name, Vendor vendor) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Fresh product for authorization tests");
        product.setUnit("kg");
        product.setCategory(ProductCategory.FRUITS);
        product.setVendor(vendor);
        return product;
    }
}
