package com.proxio.backend.controllers.ui;

import com.proxio.backend.controllers.forms.SignupForm;
import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.models.Customer;
import com.proxio.backend.models.User;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.enums.Role;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.VendorRepository;
import com.proxio.backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ViewAuthController {

    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;

    public ViewAuthController(UserService userService, CustomerRepository customerRepository, VendorRepository vendorRepository) {
        this.userService = userService;
        this.customerRepository = customerRepository;
        this.vendorRepository = vendorRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/ui/users";
        }
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VENDOR"))) {
            return "redirect:/vendor";
        }
        return "redirect:/market";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String register(
            @Valid @ModelAttribute SignupForm signupForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        User user = new User();
        user.setFullName(signupForm.getFullName());
        user.setEmail(signupForm.getEmail());
        user.setPassword(signupForm.getPassword());
        boolean vendorAccount = "VENDOR".equalsIgnoreCase(signupForm.getAccountType());
        if (vendorAccount && (signupForm.getFarmName() == null || signupForm.getFarmName().isBlank())) {
            bindingResult.rejectValue("farmName", "required", "Farm name is required for vendor accounts.");
            return "auth/signup";
        }
        user.setRole(vendorAccount ? Role.VENDOR : Role.USER);

        try {
            User saved = userService.create(user);
            if (vendorAccount) {
                Vendor vendor = new Vendor();
                vendor.setUser(saved);
                vendor.setFarmName(signupForm.getFarmName());
                vendorRepository.save(vendor);
            } else {
                Customer customer = new Customer();
                customer.setUser(saved);
                customerRepository.save(customer);
            }
        } catch (CreateOperationException exception) {
            bindingResult.rejectValue("email", "duplicate", exception.getMessage());
            return "auth/signup";
        }

        redirectAttributes.addFlashAttribute("message", "Account created. You can log in now.");
        return "redirect:/login";
    }
}
