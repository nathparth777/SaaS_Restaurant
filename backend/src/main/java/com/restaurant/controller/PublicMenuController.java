package com.restaurant.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/public/menu")
public class PublicMenuController {

    private final JdbcTemplate jdbcTemplate;

    public PublicMenuController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return jdbcTemplate.query("""
                SELECT id, name, description, display_order
                FROM menu_categories
                WHERE is_active = TRUE
                ORDER BY display_order, name
                """,
                (rs, row) -> new CategoryResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("display_order")));
    }

    @GetMapping("/items")
    public List<MenuItemResponse> items(
            @RequestParam(required = false) UUID categoryId) {
        String sql = """
                SELECT id, category_id, name, description, price, image_url
                FROM menu_items
                WHERE is_available = TRUE
                """ + (categoryId == null ? "" : " AND category_id = ?")
                + " ORDER BY name";

        return categoryId == null
                ? jdbcTemplate.query(sql, (rs, row) -> mapItem(rs))
                : jdbcTemplate.query(sql, (rs, row) -> mapItem(rs), categoryId);
    }

    private MenuItemResponse mapItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new MenuItemResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("category_id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getString("image_url"));
    }

    public record CategoryResponse(UUID id, String name, String description, int displayOrder) {}
    public record MenuItemResponse(UUID id, UUID categoryId, String name,
                                   String description, BigDecimal price, String imageUrl) {}
}
