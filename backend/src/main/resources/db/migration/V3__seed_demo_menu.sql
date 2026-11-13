-- Demo menu data matching the initial customer storefront prototype.
INSERT INTO menu_categories (id, name, description, display_order) VALUES
('10000000-0000-4000-8000-000000000001', 'Starters', 'Small plates to begin', 1),
('10000000-0000-4000-8000-000000000002', 'Mains', 'Freshly prepared favourites', 2),
('10000000-0000-4000-8000-000000000003', 'Pizza', 'Hand-stretched pizzas', 3),
('10000000-0000-4000-8000-000000000004', 'Drinks', 'Cold and refreshing drinks', 4),
('10000000-0000-4000-8000-000000000005', 'Desserts', 'A sweet finish', 5);

INSERT INTO menu_items (id, category_id, name, description, price, is_available) VALUES
('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Truffle Parmesan Fries', 'Crispy golden fries, parmesan, herbs and truffle aioli.', 189.00, TRUE),
('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Crispy Lotus Stem', 'Sweet chilli glaze, toasted sesame and spring onion.', 219.00, TRUE),
('20000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000002', 'Garden Pesto Bowl', 'Basil pesto, roasted vegetables, cherry tomatoes and feta.', 329.00, TRUE),
('20000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000002', 'Smoky Paneer Tikka', 'Chargrilled paneer, peppers, mint yoghurt and pickled onion.', 349.00, TRUE),
('20000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000003', 'Classic Margherita', 'San Marzano tomato, fresh mozzarella and basil.', 399.00, TRUE),
('20000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000003', 'Wild Mushroom Pizza', 'Roasted mushrooms, mozzarella, thyme and garlic cream.', 469.00, TRUE),
('20000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000004', 'Peach Iced Tea', 'Freshly brewed tea, peach and a hint of lemon.', 129.00, TRUE),
('20000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000004', 'Cold Coffee', 'Slow-brewed coffee, chilled milk and vanilla.', 149.00, TRUE),
('20000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000005', 'Lotus Biscoff Cheesecake', 'Creamy baked cheesecake with a caramel biscuit crumb.', 229.00, TRUE);
