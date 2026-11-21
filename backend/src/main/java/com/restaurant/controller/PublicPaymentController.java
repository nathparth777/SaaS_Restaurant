package com.restaurant.controller;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class PublicPaymentController {

    private final JdbcTemplate jdbc;
    private final RazorpayClient razorpay;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public PublicPaymentController(
            JdbcTemplate jdbc,
            ObjectProvider<RazorpayClient> razorpayProvider,
            @Value("${RAZORPAY_KEY_ID:}") String keyId,
            @Value("${RAZORPAY_KEY_SECRET:}") String keySecret,
            @Value("${RAZORPAY_WEBHOOK_SECRET:}") String webhookSecret) {
        this.jdbc = jdbc;
        this.razorpay = razorpayProvider.getIfAvailable();
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/orders/{orderId}/payment")
    @Transactional
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @PathVariable UUID orderId,
            @RequestBody AccessRequest request) throws Exception {
        requireConfigured();

        Map<String, Object> order = findOrder(orderId, request.accessToken());
        String status = (String) order.get("order_status");
        if (!"AWAITING_PAYMENT".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This order is no longer awaiting payment.");
        }

        BigDecimal amount = (BigDecimal) order.get("total_amount");
        long amountInPaise = amount.movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        if (amountInPaise <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The order amount must be greater than zero.");
        }

        JSONObject options = new JSONObject();
        options.put("amount", amountInPaise);
        options.put("currency", "INR");
        options.put("receipt", order.get("order_number").toString());
        options.put("notes", new JSONObject().put("restaurant_order_id", orderId.toString()));

        JSONObject gatewayOrder = razorpay.orders.create(options);
        String gatewayOrderId = gatewayOrder.getString("id");
        jdbc.update("""
                INSERT INTO payments
                    (id, order_id, payment_method, payment_status, amount, gateway_order_id)
                VALUES (?, ?, 'RAZORPAY', 'PENDING', ?, ?)
                """, UUID.randomUUID(), orderId, amount, gatewayOrderId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CreatePaymentResponse(orderId, order.get("order_number").toString(),
                        gatewayOrderId, amountInPaise, "INR", keyId));
    }

    @PostMapping("/orders/{orderId}/payment/verify")
    @Transactional
    public PaymentVerificationResponse verifyPayment(
            @PathVariable UUID orderId,
            @RequestBody VerifyPaymentRequest request) throws Exception {
        requireConfigured();
        Map<String, Object> order = findOrder(orderId, request.accessToken());

        Map<String, Object> paymentRecord;
        try {
            paymentRecord = jdbc.queryForMap("""
                    SELECT id, amount, gateway_order_id
                    FROM payments
                    WHERE order_id = ? AND gateway_order_id = ?
                    """, orderId, request.razorpayOrderId());
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment order does not belong to this restaurant order.");
        }

        String storedGatewayOrderId = (String) paymentRecord.get("gateway_order_id");
        JSONObject signatureData = new JSONObject()
                .put("razorpay_order_id", storedGatewayOrderId)
                .put("razorpay_payment_id", request.razorpayPaymentId())
                .put("razorpay_signature", request.razorpaySignature());
        if (!Utils.verifyPaymentSignature(signatureData, keySecret)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Razorpay payment signature could not be verified.");
        }

        var gatewayPayment = razorpay.payments.fetch(request.razorpayPaymentId());
        String gatewayPaymentOrderId = gatewayPayment.get("order_id").toString();
        String gatewayStatus = gatewayPayment.get("status").toString();
        long gatewayAmount = Long.parseLong(gatewayPayment.get("amount").toString());
        long expectedAmount = ((BigDecimal) paymentRecord.get("amount"))
                .movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();

        if (!storedGatewayOrderId.equals(gatewayPaymentOrderId)
                || gatewayAmount != expectedAmount
                || !"INR".equalsIgnoreCase(gatewayPayment.get("currency").toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment details do not match this order.");
        }

        if ("captured".equalsIgnoreCase(gatewayStatus)) {
            markPaymentCaptured(storedGatewayOrderId, request.razorpayPaymentId(),
                    request.razorpaySignature());
        }
        return new PaymentVerificationResponse(orderId,
                "captured".equalsIgnoreCase(gatewayStatus) ? "SUCCESS" : "PENDING",
                "captured".equalsIgnoreCase(gatewayStatus)
                        ? "Payment verified. Your order is awaiting restaurant confirmation."
                        : "Payment is not captured yet. Please wait for confirmation.");
    }

    @PostMapping("/payments/razorpay/webhook")
    @Transactional
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) throws Exception {
        if (webhookSecret.isBlank() || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Webhook signature is required.");
        }
        Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);

        JSONObject event = new JSONObject(rawBody);
        String eventType = event.optString("event");
        if (!"payment.captured".equals(eventType) && !"payment.failed".equals(eventType)) {
            return ResponseEntity.ok().build();
        }
        JSONObject payment = event.getJSONObject("payload")
                .getJSONObject("payment").getJSONObject("entity");
        String gatewayOrderId = payment.optString("order_id");
        String gatewayPaymentId = payment.optString("id");
        if (gatewayOrderId.isBlank() || gatewayPaymentId.isBlank()) {
            return ResponseEntity.ok().build();
        }

        String idempotencyKey = (eventId == null || eventId.isBlank())
                ? gatewayPaymentId + ":" + eventType : eventId;
        int inserted = jdbc.update("""
                INSERT INTO processed_payment_webhooks(event_id)
                VALUES (?)
                ON CONFLICT (event_id) DO NOTHING
                """, idempotencyKey);
        if (inserted == 0) return ResponseEntity.ok().build();

        if ("payment.captured".equals(eventType)) {
            markPaymentCaptured(gatewayOrderId, gatewayPaymentId, null);
        } else {
            jdbc.update("""
                    UPDATE payments SET payment_status = 'FAILED', gateway_payment_id = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE gateway_order_id = ? AND payment_status <> 'SUCCESS'
                    """, gatewayPaymentId, gatewayOrderId);
        }
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> findOrder(UUID orderId, UUID accessToken) {
        if (accessToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order access token is required.");
        }
        try {
            return jdbc.queryForMap("""
                    SELECT id, order_number, order_status, total_amount
                    FROM customer_orders
                    WHERE id = ? AND access_token = ?
                    """, orderId, accessToken);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
        }
    }

    private void markPaymentCaptured(String gatewayOrderId, String paymentId, String signature) {
        int updated = jdbc.update("""
                UPDATE payments SET payment_status = 'SUCCESS',
                    gateway_payment_id = ?, gateway_signature = ?,
                    transaction_reference = ?, updated_at = CURRENT_TIMESTAMP
                WHERE gateway_order_id = ?
                """, paymentId, signature, paymentId, gatewayOrderId);
        if (updated > 0) {
            jdbc.update("""
                    UPDATE customer_orders SET order_status = 'PENDING_ACCEPTANCE',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = (SELECT order_id FROM payments WHERE gateway_order_id = ?)
                      AND order_status = 'AWAITING_PAYMENT'
                    """, gatewayOrderId);
        }
    }

    private void requireConfigured() {
        if (razorpay == null || keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payment is not configured. Please contact the restaurant.");
        }
    }

    public record AccessRequest(UUID accessToken) {}
    public record VerifyPaymentRequest(
            UUID accessToken, String razorpayOrderId,
            String razorpayPaymentId, String razorpaySignature) {}
    public record CreatePaymentResponse(
            UUID orderId, String orderNumber, String razorpayOrderId,
            long amount, String currency, String keyId) {}
    public record PaymentVerificationResponse(
            UUID orderId, String paymentStatus, String message) {}
}
