package com.restaurant.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Bean
    @ConditionalOnExpression("'${RAZORPAY_KEY_ID:}' != '' && '${RAZORPAY_KEY_SECRET:}' != ''")
    RazorpayClient razorpayClient(
            @Value("${RAZORPAY_KEY_ID}") String keyId,
            @Value("${RAZORPAY_KEY_SECRET}") String keySecret) throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
