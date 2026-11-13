package com.restaurant.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/public/orders")
public class PublicOrderController {

    private final JdbcTemplate jdbcTemplate;

    public PublicOrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        UUID orderId = UUID.randomUUID();
        UUID accessToken = UUID.randomUUID();
        String orderNumber = "ORD-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest line : request.items()) {
            quantities.merge(line.menuItemId(), line.quantity(), Integer::sum);
        }

        BigDecimal total = BigDecimal.ZERO;
        record PricedLine(UUID id, String name, BigDecimal price, int quantity) {}
        List<PricedLine> pricedLines = new java.util.ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            try {
                Map<String, Object> item = jdbcTemplate.queryForMap(
                        "SELECT name, price FROM menu_items WHERE id = ? AND is_available = TRUE",
                        entry.getKey());
                String name = (String) item.get("name");
                BigDecimal price = (BigDecimal) item.get("price");
                pricedLines.add(new PricedLine(entry.getKey(), name, price, entry.getValue()));
                total = total.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
            } catch (EmptyResultDataAccessException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "One or more menu items are unavailable.");
            }
        }

        jdbcTemplate.update("""
                INSERT INTO customer_orders
                    (id, order_number, table_id, table_number, customer_name,
                     customer_phone, order_status, total_amount, access_token)
                VALUES (?, ?, NULL, ?, ?, ?, 'AWAITING_PAYMENT', ?, ?)
                """,
                orderId, orderNumber, request.tableNumber().trim(),
                request.customerName().trim(), request.phoneNumber(), total, accessToken);

        for (PricedLine line : pricedLines) {
            BigDecimal lineTotal = line.price().multiply(BigDecimal.valueOf(line.quantity()));
            jdbcTemplate.update("""
                    INSERT INTO order_items
                        (id, order_id, menu_item_id, item_name, unit_price, quantity, line_total)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(), orderId, line.id(), line.name(),
                    line.price(), line.quantity(), lineTotal);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CreateOrderResponse(orderId, orderNumber, accessToken,
                        "AWAITING_PAYMENT", total));
    }

    public record CreateOrderRequest(
            @NotBlank @Size(max = 30) String tableNumber,
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$",
                    message = "Phone number must contain exactly 10 digits.")
            String phoneNumber,
            @NotEmpty @Size(max = 50) List<@Valid OrderLineRequest> items) {}

    public record OrderLineRequest(
            @jakarta.validation.constraints.NotNull UUID menuItemId,
            @Min(1) @jakarta.validation.constraints.Max(99) int quantity) {}

    public record CreateOrderResponse(
            UUID orderId,
            String orderNumber,
            UUID accessToken,
            String status,
            BigDecimal totalAmount) {}
}
