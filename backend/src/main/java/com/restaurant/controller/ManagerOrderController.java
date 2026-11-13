package com.restaurant.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/manager/orders")
@CrossOrigin(origins = "http://localhost:5174")
public class ManagerOrderController {

    private final JdbcTemplate jdbcTemplate;

    public ManagerOrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<OrderSummary> listOrders() {
        return jdbcTemplate.query("""
                SELECT id, order_number, table_number, customer_name, customer_phone,
                       order_status, total_amount, created_at
                FROM customer_orders
                ORDER BY created_at DESC
                LIMIT 100
                """, (rs, row) -> new OrderSummary(
                rs.getObject("id", UUID.class),
                rs.getString("order_number"),
                rs.getString("table_number"),
                rs.getString("customer_name"),
                rs.getString("customer_phone"),
                rs.getString("order_status"),
                rs.getBigDecimal("total_amount"),
                rs.getObject("created_at", OffsetDateTime.class),
                itemsFor(rs.getObject("id", UUID.class))));
    }

    private List<OrderItemSummary> itemsFor(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT item_name, unit_price, quantity, line_total
                FROM order_items
                WHERE order_id = ?
                ORDER BY item_name
                """, (rs, row) -> new OrderItemSummary(
                rs.getString("item_name"),
                rs.getBigDecimal("unit_price"),
                rs.getInt("quantity"),
                rs.getBigDecimal("line_total")), orderId);
    }

    public record OrderSummary(
            UUID id, String orderNumber, String tableNumber, String customerName,
            String customerPhone, String status, BigDecimal totalAmount,
            OffsetDateTime createdAt, List<OrderItemSummary> items) {}

    public record OrderItemSummary(
            String itemName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
}
