package com.proxio.backend.controllers;

import com.proxio.backend.models.Customer;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.services.CustomerService;
import com.proxio.backend.services.UserService;
import com.proxio.backend.services.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final VendorService vendorService;
    private final CustomerService customerService;

    public AuthController(UserService userService, VendorService vendorService, CustomerService customerService) {
        this.userService = userService;
        this.vendorService = vendorService;
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User savedUser = userService.create(user);

        switch (savedUser.getRole()) {
            case VENDOR -> {
                Vendor vendor = new Vendor();
                vendor.setUser(savedUser);
                vendor.setFarmName(savedUser.getFullName() + "'s farm");
                vendorService.create(vendor);
            }
            case CUSTOMER -> {
                Customer customer = new Customer();
                customer.setUser(savedUser);
                customerService.create(customer);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
