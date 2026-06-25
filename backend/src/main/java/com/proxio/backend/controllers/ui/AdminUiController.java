package com.proxio.backend.controllers.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui")
public class AdminUiController {

    private final EntityUiService entityUiService;

    public AdminUiController(EntityUiService entityUiService) {
        this.entityUiService = entityUiService;
    }

    @GetMapping
    public String dashboard() {
        return "redirect:/ui/users";
    }

    @GetMapping("/{entityKey}")
    public String list(
            @PathVariable String entityKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model
    ) {
        EntityUiService.EntityDefinition definition = entityUiService.definition(entityKey);
        Page<?> records = entityUiService.page(entityKey, page, size, sort, direction);
        addSharedModel(model, definition);
        model.addAttribute("records", records);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("size", size);
        return "ui/list";
    }

    @GetMapping("/{entityKey}/new")
    public String createForm(@PathVariable String entityKey, Model model) {
        EntityUiService.EntityDefinition definition = entityUiService.definition(entityKey);
        addSharedModel(model, definition);
        model.addAttribute("record", entityUiService.newEntity(entityKey));
        model.addAttribute("fields", entityUiService.formFields(entityKey));
        model.addAttribute("errors", java.util.List.of());
        model.addAttribute("mode", "Create");
        return "ui/form";
    }

    @PostMapping("/{entityKey}")
    public String create(
            @PathVariable String entityKey,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        return save(entityKey, null, request, redirectAttributes, model);
    }

    @GetMapping("/{entityKey}/{id}/edit")
    public String editForm(@PathVariable String entityKey, @PathVariable Long id, Model model) {
        EntityUiService.EntityDefinition definition = entityUiService.definition(entityKey);
        addSharedModel(model, definition);
        model.addAttribute("record", entityUiService.find(entityKey, id));
        model.addAttribute("fields", entityUiService.formFields(entityKey));
        model.addAttribute("errors", java.util.List.of());
        model.addAttribute("mode", "Edit");
        return "ui/form";
    }

    @PostMapping("/{entityKey}/{id}")
    public String update(
            @PathVariable String entityKey,
            @PathVariable Long id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        return save(entityKey, id, request, redirectAttributes, model);
    }

    @PostMapping("/{entityKey}/{id}/delete")
    public String delete(@PathVariable String entityKey, @PathVariable Long id, RedirectAttributes redirectAttributes) {
        entityUiService.delete(entityKey, id);
        redirectAttributes.addFlashAttribute("message", "Record deleted.");
        return "redirect:/ui/" + entityKey;
    }

    private String save(
            String entityKey,
            Long id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        EntityUiService.EntityDefinition definition = entityUiService.definition(entityKey);
        EntityUiService.SaveResult result = entityUiService.save(entityKey, id, request.getParameterMap());
        if (!result.valid()) {
            addSharedModel(model, definition);
            model.addAttribute("record", result.entity());
            model.addAttribute("fields", entityUiService.formFields(entityKey));
            model.addAttribute("errors", result.errors());
            model.addAttribute("mode", id == null ? "Create" : "Edit");
            return "ui/form";
        }

        redirectAttributes.addFlashAttribute("message", "Record saved.");
        return "redirect:/ui/" + entityKey;
    }

    private void addSharedModel(Model model, EntityUiService.EntityDefinition definition) {
        model.addAttribute("entities", entityUiService.allDefinitions());
        model.addAttribute("definition", definition);
        model.addAttribute("ui", entityUiService);
    }
}
