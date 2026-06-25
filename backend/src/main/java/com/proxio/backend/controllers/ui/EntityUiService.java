package com.proxio.backend.controllers.ui;

import com.proxio.backend.models.Customer;
import com.proxio.backend.models.Location;
import com.proxio.backend.models.Order;
import com.proxio.backend.models.OrderItem;
import com.proxio.backend.models.PickupSlot;
import com.proxio.backend.models.Product;
import com.proxio.backend.models.Stock;
import com.proxio.backend.models.Subscription;
import com.proxio.backend.models.User;
import com.proxio.backend.models.UserRating;
import com.proxio.backend.models.Vendor;
import com.proxio.backend.models.WeeklyOffer;
import com.proxio.backend.repositories.CustomerRepository;
import com.proxio.backend.repositories.LocationRepository;
import com.proxio.backend.repositories.OrderItemRepository;
import com.proxio.backend.repositories.OrderRepository;
import com.proxio.backend.repositories.PickupSlotRepository;
import com.proxio.backend.repositories.ProductRepository;
import com.proxio.backend.repositories.StockRepository;
import com.proxio.backend.repositories.SubscriptionRepository;
import com.proxio.backend.repositories.UserRatingRepository;
import com.proxio.backend.repositories.UserRepository;
import com.proxio.backend.repositories.VendorRepository;
import com.proxio.backend.repositories.WeeklyOfferRepository;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EntityUiService {

    private final Map<String, EntityDefinition> definitions = new LinkedHashMap<>();
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;

    public EntityUiService(
            UserRepository userRepository,
            VendorRepository vendorRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            LocationRepository locationRepository,
            StockRepository stockRepository,
            WeeklyOfferRepository weeklyOfferRepository,
            PickupSlotRepository pickupSlotRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            SubscriptionRepository subscriptionRepository,
            UserRatingRepository userRatingRepository,
            Validator validator,
            PasswordEncoder passwordEncoder
    ) {
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
        register("users", "Users", User.class, userRepository, "fullName", "email", "role");
        register("vendors", "Vendors", Vendor.class, vendorRepository, "farmName", "description", "user");
        register("customers", "Customers", Customer.class, customerRepository, "user", "phone", "address");
        register("products", "Products", Product.class, productRepository, "name", "category", "vendor");
        register("locations", "Locations", Location.class, locationRepository, "name", "city", "vendor");
        register("stocks", "Stocks", Stock.class, stockRepository, "product", "location", "quantity");
        register("weekly-offers", "Weekly Offers", WeeklyOffer.class, weeklyOfferRepository, "product", "location", "pricePerUnit");
        register("pickup-slots", "Pickup Slots", PickupSlot.class, pickupSlotRepository, "location", "startTime", "endTime");
        register("orders", "Orders", Order.class, orderRepository, "customer", "location", "status");
        register("order-items", "Order Items", OrderItem.class, orderItemRepository, "order", "weeklyOffer", "quantity");
        register("subscriptions", "Subscriptions", Subscription.class, subscriptionRepository, "customer", "vendor", "notificationsEnabled");
        register("user-ratings", "User Ratings", UserRating.class, userRatingRepository, "rater", "rated", "score");
    }

    public List<EntityDefinition> allDefinitions() {
        return new ArrayList<>(definitions.values());
    }

    public EntityDefinition definition(String key) {
        EntityDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown entity: " + key);
        }
        return definition;
    }

    public Page<?> page(String key, int page, int size, String sort, String direction) {
        EntityDefinition definition = definition(key);
        String sortProperty = definition.sortFields().contains(sort) ? sort : definition.sortFields().get(0);
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return definition.repository().findAll(PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(sortDirection, sortProperty)));
    }

    public Object newEntity(String key) {
        try {
            return definition(key).entityClass().getDeclaredConstructor().newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create entity form.", exception);
        }
    }

    public Object find(String key, Long id) {
        Object entity = definition(key).repository().findById(id).orElse(null);
        if (entity == null) {
            throw new IllegalArgumentException("Record not found.");
        }
        return entity;
    }

    public SaveResult save(String key, Long id, Map<String, String[]> parameters) {
        EntityDefinition definition = definition(key);
        Object entity = id == null ? newEntity(key) : find(key, id);
        bind(definition, entity, parameters, id == null);

        Set<ConstraintViolation<Object>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            return SaveResult.invalid(entity, violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .toList());
        }

        return SaveResult.valid(definition.repository().save(entity));
    }

    public void delete(String key, Long id) {
        definition(key).repository().deleteById(id);
    }

    public List<FormField> formFields(String key) {
        EntityDefinition definition = definition(key);
        List<FormField> fields = new ArrayList<>();
        for (Field field : allFields(definition.entityClass())) {
            if (isSkipped(field)) {
                continue;
            }
            if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                fields.add(FormField.relation(field.getName(), label(field.getName()), field.getType(), false));
            } else if (field.isAnnotationPresent(ManyToMany.class)) {
                fields.add(FormField.relation(field.getName(), label(field.getName()), collectionType(field), true));
            } else if (field.getType().isEnum()) {
                fields.add(FormField.options(field.getName(), label(field.getName()), Arrays.asList(field.getType().getEnumConstants())));
            } else if (isEditableSimple(field.getType())) {
                fields.add(FormField.simple(field.getName(), label(field.getName()), inputType(field.getType())));
            }
        }
        return fields;
    }

    public List<?> relationOptions(Class<?> entityClass) {
        return definitions.values().stream()
                .filter(definition -> definition.entityClass().equals(entityClass))
                .findFirst()
                .map(definition -> definition.repository().findAll(Sort.by("id")))
                .orElse(List.of());
    }

    public Object property(Object item, String property) {
        if (item == null || !StringUtils.hasText(property)) {
            return null;
        }
        return new BeanWrapperImpl(item).getPropertyValue(property);
    }

    public Long idOf(Object item) {
        if (item == null) {
            return null;
        }
        Object id = property(item, "id");
        return id instanceof Long value ? value : null;
    }

    public String display(Object item) {
        if (item == null) {
            return "";
        }
        BeanWrapper wrapper = new BeanWrapperImpl(item);
        for (String property : List.of("fullName", "farmName", "name", "email", "id")) {
            if (wrapper.isReadableProperty(property)) {
                Object value = wrapper.getPropertyValue(property);
                if (value != null && StringUtils.hasText(value.toString())) {
                    return value.toString();
                }
            }
        }
        return item.toString();
    }

    public boolean selected(Object entity, FormField field, Object option) {
        Object current = property(entity, field.name());
        Long optionId = idOf(option);
        if (current == null || optionId == null) {
            return false;
        }
        if (current instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> optionId.equals(idOf(item)));
        }
        return optionId.equals(idOf(current));
    }

    private void register(String key, String title, Class<?> entityClass, JpaRepository<?, Long> repository, String... tableFields) {
        definitions.put(key, new EntityDefinition(key, title, entityClass, repository, List.of(tableFields), sortFields(entityClass, tableFields)));
    }

    private void bind(EntityDefinition definition, Object entity, Map<String, String[]> parameters, boolean creating) {
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        for (FormField field : formFields(definition.key())) {
            if (!wrapper.isWritableProperty(field.name())) {
                continue;
            }
            String[] values = parameters.get(field.name());
            if ("password".equals(field.name()) && !creating && isBlank(values)) {
                continue;
            }
            if (field.relation()) {
                wrapper.setPropertyValue(field.name(), relationValue(field, values));
            } else if (field.multiple()) {
                wrapper.setPropertyValue(field.name(), relationValue(field, values));
            } else {
                Object value = simpleValue(wrapper.getPropertyType(field.name()), values);
                if ("password".equals(field.name()) && value instanceof String password) {
                    value = passwordEncoder.encode(password);
                }
                wrapper.setPropertyValue(field.name(), value);
            }
        }
    }

    private Object relationValue(FormField field, String[] values) {
        List<Long> ids = parseIds(values);
        if (field.multiple()) {
            Set<Object> selected = new LinkedHashSet<>();
            for (Long id : ids) {
                selected.add(findByEntityClass(field.relatedType(), id));
            }
            return selected;
        }
        if (ids.isEmpty()) {
            return null;
        }
        return findByEntityClass(field.relatedType(), ids.get(0));
    }

    private Object findByEntityClass(Class<?> entityClass, Long id) {
        EntityDefinition definition = definitions.values().stream()
                .filter(candidate -> candidate.entityClass().equals(entityClass))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No repository for " + entityClass.getSimpleName()));
        Object entity = definition.repository().findById(id).orElse(null);
        if (entity == null) {
            throw new IllegalArgumentException("Related record not found.");
        }
        return entity;
    }

    private Object simpleValue(Class<?> type, String[] values) {
        if (isBlank(values)) {
            return type.equals(boolean.class) ? false : null;
        }
        String value = values[0];
        if (type.equals(String.class)) {
            return value;
        }
        if (type.equals(Long.class) || type.equals(long.class)) {
            return Long.valueOf(value);
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.valueOf(value);
        }
        if (type.equals(Double.class) || type.equals(double.class)) {
            return Double.valueOf(value);
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return Boolean.valueOf(value);
        }
        if (type.equals(LocalDate.class)) {
            return LocalDate.parse(value);
        }
        if (type.equals(LocalDateTime.class)) {
            return LocalDateTime.parse(value);
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, value);
        }
        return null;
    }

    private List<Long> parseIds(String[] values) {
        if (values == null) {
            return List.of();
        }
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }

    private boolean isSkipped(Field field) {
        return field.getName().equals("id")
                || field.getName().equals("createdAt")
                || field.getName().equals("updatedAt")
                || field.getName().equals("subscribedAt")
                || field.getName().equals("moderatedAt")
                || (Collection.class.isAssignableFrom(field.getType()) && !field.isAnnotationPresent(ManyToMany.class))
                || (field.isAnnotationPresent(ManyToMany.class) && StringUtils.hasText(field.getAnnotation(ManyToMany.class).mappedBy()));
    }

    private boolean isEditableSimple(Class<?> type) {
        return type.equals(String.class)
                || type.equals(Long.class)
                || type.equals(long.class)
                || type.equals(Integer.class)
                || type.equals(int.class)
                || type.equals(Double.class)
                || type.equals(double.class)
                || type.equals(Boolean.class)
                || type.equals(boolean.class)
                || type.equals(LocalDate.class)
                || type.equals(LocalDateTime.class);
    }

    private String inputType(Class<?> type) {
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "checkbox";
        }
        if (type.equals(LocalDate.class)) {
            return "date";
        }
        if (type.equals(LocalDateTime.class)) {
            return "datetime-local";
        }
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
            return "number";
        }
        return "text";
    }

    private Class<?> collectionType(Field field) {
        String typeName = field.getGenericType().getTypeName();
        return definitions.values().stream()
                .map(EntityDefinition::entityClass)
                .filter(entityClass -> typeName.contains(entityClass.getName()))
                .findFirst()
                .orElse(Object.class);
    }

    private List<Field> allFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = entityClass;
        while (current != null && !current.equals(Object.class)) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private List<String> sortFields(Class<?> entityClass, String[] tableFields) {
        List<String> sortFields = new ArrayList<>();
        sortFields.add("id");
        for (String field : tableFields) {
            if (fieldExists(entityClass, field) && isSortable(entityClass, field)) {
                sortFields.add(field);
            }
        }
        return sortFields.stream().distinct().limit(3).toList();
    }

    private boolean fieldExists(Class<?> entityClass, String name) {
        return allFields(entityClass).stream().anyMatch(field -> field.getName().equals(name));
    }

    private boolean isSortable(Class<?> entityClass, String name) {
        return allFields(entityClass).stream()
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .map(field -> isEditableSimple(field.getType()) || field.getType().isEnum())
                .orElse(false);
    }

    private String label(String name) {
        StringBuilder label = new StringBuilder();
        for (char character : name.toCharArray()) {
            if (Character.isUpperCase(character)) {
                label.append(' ');
            }
            label.append(character);
        }
        label.setCharAt(0, Character.toUpperCase(label.charAt(0)));
        return label.toString();
    }

    private boolean isBlank(String[] values) {
        return values == null || values.length == 0 || !StringUtils.hasText(values[0]);
    }

    public record EntityDefinition(
            String key,
            String title,
            Class<?> entityClass,
            JpaRepository repository,
            List<String> tableFields,
            List<String> sortFields
    ) {
    }

    public record FormField(
            String name,
            String label,
            String inputType,
            boolean relation,
            boolean multiple,
            Class<?> relatedType,
            List<?> options
    ) {
        static FormField simple(String name, String label, String inputType) {
            return new FormField(name, label, inputType, false, false, null, List.of());
        }

        static FormField relation(String name, String label, Class<?> relatedType, boolean multiple) {
            return new FormField(name, label, "select", true, multiple, relatedType, List.of());
        }

        static FormField options(String name, String label, List<?> options) {
            return new FormField(name, label, "select", false, false, null, options);
        }
    }

    public record SaveResult(Object entity, boolean valid, List<String> errors) {
        static SaveResult valid(Object entity) {
            return new SaveResult(entity, true, List.of());
        }

        static SaveResult invalid(Object entity, List<String> errors) {
            return new SaveResult(entity, false, errors);
        }
    }
}
