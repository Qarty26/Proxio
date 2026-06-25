package com.proxio.backend.controllers.ui;

import com.proxio.backend.controllers.forms.CheckoutForm;
import com.proxio.backend.controllers.forms.RatingForm;
import com.proxio.backend.exceptions.CreateOperationException;
import com.proxio.backend.models.Order;
import com.proxio.backend.models.OrderItem;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.models.enums.Role;
import com.proxio.backend.services.MarketplaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping("/market")
    public String market(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long vendorId,
            Principal principal,
            Model model,
            Authentication authentication
    ) {
        int pageSize = Math.min(Math.max(size, 3), 12);
        Page<WeeklyOffer> offers = marketplaceService.activeOffers(
                locationId,
                vendorId,
                PageRequest.of(Math.max(page, 0), pageSize, Sort.by(Sort.Direction.ASC, "validUntil"))
        );
        addNavigation(model, authentication);
        model.addAttribute("offers", offers);
        model.addAttribute("size", pageSize);
        model.addAttribute("pageSizes", java.util.List.of(3, 6, 9, 12));
        model.addAttribute("locations", marketplaceService.locations());
        model.addAttribute("selectedLocationId", locationId);
        model.addAttribute("selectedVendorId", vendorId);
        model.addAttribute("principalName", principal.getName());
        model.addAttribute("market", this);
        return "market/offers";
    }

    @GetMapping("/offers/{id}")
    public String offerDetails(@PathVariable Long id, Model model, Authentication authentication) {
        addNavigation(model, authentication);
        model.addAttribute("offer", marketplaceService.offer(id));
        model.addAttribute("pickupSlots", marketplaceService.pickupSlotsForOffer(id));
        model.addAttribute("checkoutForm", new CheckoutForm());
        return "market/offer-details";
    }

    @PostMapping("/offers/{id}/order")
    public String placeOrder(
            @PathVariable Long id,
            @Valid @ModelAttribute CheckoutForm checkoutForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addNavigation(model, authentication);
            model.addAttribute("offer", marketplaceService.offer(id));
            model.addAttribute("pickupSlots", marketplaceService.pickupSlotsForOffer(id));
            return "market/offer-details";
        }

        try {
            Order order = marketplaceService.placeOrder(
                    principal.getName(),
                    id,
                    checkoutForm.getQuantity(),
                    checkoutForm.getPickupSlotId()
            );
            redirectAttributes.addFlashAttribute("message", "Order #" + order.getId() + " was placed.");
            return "redirect:/orders";
        } catch (CreateOperationException exception) {
            addNavigation(model, authentication);
            model.addAttribute("offer", marketplaceService.offer(id));
            model.addAttribute("pickupSlots", marketplaceService.pickupSlotsForOffer(id));
            model.addAttribute("orderError", exception.getMessage());
            return "market/offer-details";
        }
    }

    @GetMapping("/orders")
    public String myOrders(
            @RequestParam(required = false) Long rateOrder,
            Principal principal,
            Model model,
            Authentication authentication
    ) {
        addNavigation(model, authentication);
        model.addAttribute("orders", marketplaceService.ordersFor(principal.getName()));
        model.addAttribute("ratingForm", new RatingForm());
        model.addAttribute("rateOrderId", rateOrder);
        model.addAttribute("principalName", principal.getName());
        model.addAttribute("market", this);
        return "market/orders";
    }

    @PostMapping("/orders/{id}/done")
    public String markDone(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            marketplaceService.markDone(principal.getName(), id);
            redirectAttributes.addAttribute("rateOrder", id);
            redirectAttributes.addFlashAttribute("message", "Order marked as done. Please rate the vendor.");
        } catch (CreateOperationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/rating")
    public String rateOrder(
            @PathVariable Long id,
            @Valid @ModelAttribute RatingForm ratingForm,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Choose a rating between 1 and 5.");
            return "redirect:/orders";
        }
        try {
            marketplaceService.rateOrder(principal.getName(), id, ratingForm);
            redirectAttributes.addFlashAttribute("message", "Thank you, your rating was saved.");
        } catch (CreateOperationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Principal principal, Model model, Authentication authentication) {
        addNavigation(model, authentication);
        model.addAttribute("subscriptions", marketplaceService.subscriptionsFor(principal.getName()));
        model.addAttribute("market", this);
        return "market/subscriptions";
    }

    @PostMapping("/vendors/{id}/subscribe")
    public String subscribe(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        marketplaceService.subscribe(principal.getName(), id);
        redirectAttributes.addFlashAttribute("message", "Subscription added.");
        return "redirect:/market";
    }

    @PostMapping("/vendors/{id}/unsubscribe")
    public String unsubscribe(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        marketplaceService.unsubscribe(principal.getName(), id);
        redirectAttributes.addFlashAttribute("message", "Subscription removed.");
        return "redirect:/subscriptions";
    }

    public double total(Order order) {
        if (order.getItems() == null) {
            return 0.0;
        }
        return order.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getPriceAtOrder())
                .sum();
    }

    public String itemSummary(OrderItem item) {
        return item.getWeeklyOffer().getProduct().getName()
                + " x "
                + item.getQuantity()
                + " "
                + item.getWeeklyOffer().getProduct().getUnit();
    }

    public String vendorSummary(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "Vendor not available";
        }
        return order.getItems().get(0).getWeeklyOffer().getProduct().getVendor().getFarmName();
    }

    public UserRating ratingFor(String email, Long orderId) {
        return marketplaceService.ratingFor(email, orderId);
    }

    public String ratingSummaryForVendor(Long vendorId) {
        return marketplaceService.ratingSummaryForVendor(vendorId);
    }

    public int roundedRatingForVendor(Long vendorId) {
        return marketplaceService.roundedRatingForVendor(vendorId);
    }

    public boolean subscribedTo(String email, Long vendorId) {
        return marketplaceService.subscribedTo(email, vendorId);
    }

    private void addNavigation(Model model, Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
        boolean vendor = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + Role.VENDOR.name()));
        boolean customer = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + Role.USER.name())
                        || authority.getAuthority().equals("ROLE_" + Role.CUSTOMER.name()));
        model.addAttribute("isAdmin", admin);
        model.addAttribute("isVendor", vendor);
        model.addAttribute("isCustomer", customer);
    }
}
