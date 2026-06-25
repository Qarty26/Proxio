package com.proxio.backend.controllers.ui;

import com.proxio.backend.controllers.forms.VendorProductOfferForm;
import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.models.enums.ProductCategory;
import com.proxio.backend.services.VendorPortalService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class VendorPortalController {

    private final VendorPortalService vendorPortalService;

    public VendorPortalController(VendorPortalService vendorPortalService) {
        this.vendorPortalService = vendorPortalService;
    }

    @GetMapping("/vendor")
    public String dashboard(Principal principal, Authentication authentication, Model model) {
        addNavigation(model, authentication);
        model.addAttribute("vendor", vendorPortalService.vendorFor(principal.getName()));
        model.addAttribute("products", vendorPortalService.productsFor(principal.getName()));
        model.addAttribute("offers", vendorPortalService.offersFor(principal.getName()));
        return "vendor/dashboard";
    }

    @GetMapping("/vendor/products/new")
    public String newProduct(Authentication authentication, Model model) {
        addNavigation(model, authentication);
        model.addAttribute("form", new VendorProductOfferForm());
        model.addAttribute("categories", ProductCategory.values());
        return "vendor/product-form";
    }

    @PostMapping("/vendor/products")
    public String createProduct(
            @Valid @ModelAttribute("form") VendorProductOfferForm form,
            BindingResult bindingResult,
            Principal principal,
            Authentication authentication,
            Model model,
            @RequestParam(value = "image", required = false) MultipartFile image,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addNavigation(model, authentication);
            model.addAttribute("categories", ProductCategory.values());
            return "vendor/product-form";
        }

        try {
            vendorPortalService.publishProductOffer(principal.getName(), form, image);
        } catch (CreateOperationException exception) {
            addNavigation(model, authentication);
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("formError", exception.getMessage());
            return "vendor/product-form";
        }

        redirectAttributes.addFlashAttribute("message", "Product and weekly offer published.");
        return "redirect:/vendor";
    }

    private void addNavigation(Model model, Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isVendor", true);
    }
}
