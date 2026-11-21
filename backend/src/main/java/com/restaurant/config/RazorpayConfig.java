package com.restaurant.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Bean
    RazorpayClient razorpayClient(
            @Value("${RAZORPAY_KEY_ID:}") String keyId,
            @Value("${RAZORPAY_KEY_SECRET:}") String keySecret) throws RazorpayException {
        if (keyId.isBlank() || keySecret.isBlank()) {
            return null;
        }
        return new RazorpayClient(keyId, keySecret);
    }
}
