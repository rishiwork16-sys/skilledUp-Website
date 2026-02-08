package com.skilledup.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.skilledup.payment.entity.PaymentOrder;
import com.skilledup.payment.entity.UserCourse;
import com.skilledup.payment.repository.PaymentRepository;
import com.skilledup.payment.repository.UserCourseRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private final PaymentRepository paymentRepository;
    private final UserCourseRepository userCourseRepository;
    private final com.skilledup.payment.repository.UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository, UserCourseRepository userCourseRepository,
            com.skilledup.payment.repository.UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userCourseRepository = userCourseRepository;
        this.userRepository = userRepository;
    }

    public PaymentOrder createOrder(Long userId, String email, Long courseId, Double amount) {
        try {
            // Resolve userId from email if missing
            if (userId == null && email != null) {
                com.skilledup.payment.entity.User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    userId = user.getId();
                    logger.info("Resolved userId {} from email {}", userId, email);
                }
            }

            if (userId == null) {
                throw new RuntimeException("User ID could not be determined for payment");
            }

            // Handle FREE courses (amount <= 0)
            if (amount <= 0) {
                logger.info("Course is FREE. Skipping Razorpay order creation for userId: {}, courseId: {}", userId, courseId);
                
                PaymentOrder paymentOrder = new PaymentOrder();
                paymentOrder.setOrderId("free_" + System.currentTimeMillis());
                paymentOrder.setUserId(userId);
                paymentOrder.setCourseId(courseId);
                paymentOrder.setAmount(0.0);
                paymentOrder.setStatus("PAID"); // Auto-mark as PAID
                paymentOrder.setPaymentId("free_payment_" + System.currentTimeMillis());
                
                PaymentOrder savedOrder = paymentRepository.save(paymentOrder);
                
                // Grant access immediately
                grantAccess(userId, courseId, savedOrder.getPaymentId());
                
                return savedOrder;
            }

            logger.info("Creating order for userId: {}, courseId: {}, amount: {}", userId, courseId, amount);
            logger.info("Initializing Razorpay Client with keyId: {}", keyId);
            // RazorpayClient razorpay = new RazorpayClient(keyId, keySecret); // Causing
            // issues

            // Manual implementation using RestTemplate to bypass SDK version issues
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String auth = keyId + ":" + keySecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            Map<String, Object> requestMap = new HashMap<>();
            long amountInPaise = Math.round(amount * 100);
            requestMap.put("amount", amountInPaise);
            requestMap.put("currency", "INR");
            requestMap.put("receipt", "txn_" + System.currentTimeMillis());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);

            logger.info("Sending request to Razorpay API directly: {}", requestMap);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.razorpay.com/v1/orders", entity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Razorpay API failed with status: " + response.getStatusCode());
            }

            JSONObject order = new JSONObject(response.getBody());
            logger.info("Razorpay order created: {}", order.get("id").toString());

            PaymentOrder paymentOrder = new PaymentOrder();
            paymentOrder.setOrderId(order.get("id").toString());
            paymentOrder.setUserId(userId);
            paymentOrder.setCourseId(courseId);
            paymentOrder.setAmount(amount);
            paymentOrder.setStatus("CREATED");

            logger.info("Saving PaymentOrder to DB...");
            PaymentOrder savedOrder = paymentRepository.save(paymentOrder);
            logger.info("PaymentOrder saved with ID: {}", savedOrder.getId());

            return savedOrder;
        } catch (Exception e) {
            logger.error("Unexpected error during order creation: {}", e.getMessage(), e);
            throw new RuntimeException("Internal error during payment initialization: " + e.getMessage());
        }
    }

    public PaymentOrder verifyPayment(String orderId, String paymentId, String signature) {
        logger.info("Verifying payment for orderId: {}, paymentId: {}", orderId, paymentId);
        Optional<PaymentOrder> orderOpt = paymentRepository.findByOrderId(orderId);
        if (orderOpt.isPresent()) {
            PaymentOrder order = orderOpt.get();
            try {
                // Verify signature manually using HMAC SHA256
                String data = orderId + "|" + paymentId;
                String generatedSignature = calculateHmacSha256(data, keySecret);

                boolean isValid = generatedSignature.equals(signature);

                if (isValid) {
                    logger.info("Payment signature is valid for orderId: {}", orderId);
                    order.setPaymentId(paymentId);
                    order.setSignature(signature);
                    order.setStatus("PAID");
                    PaymentOrder savedOrder = paymentRepository.save(order);

                    // Grant course access immediately
                    grantAccess(order.getUserId(), order.getCourseId(), order.getPaymentId());

                    return savedOrder;
                } else {
                    logger.warn("Invalid payment signature for orderId: {}", orderId);
                    throw new RuntimeException("Invalid Signature");
                }
            } catch (Exception e) {
                logger.error("Verification failed for orderId: {}: {}", orderId, e.getMessage());
                throw new RuntimeException("Verification Failed: " + e.getMessage());
            }
        }
        logger.warn("Order not found during verification: {}", orderId);
        throw new RuntimeException("Order not found");
    }

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public void processWebhook(String payload, String signature) {
        try {
            logger.info("Processing Razorpay webhook");

            // Manual webhook verification
            String generatedSignature = calculateHmacSha256(payload, webhookSecret);
            boolean isValid = generatedSignature.equals(signature);

            if (isValid) {
                JSONObject json = new JSONObject(payload);
                String event = json.getString("event");
                logger.info("Webhook event: {}", event);

                if ("order.paid".equals(event) || "payment.captured".equals(event)) {
                    JSONObject payloadObj = json.getJSONObject("payload");
                    JSONObject payment = payloadObj.getJSONObject("payment");
                    JSONObject entity = payment.getJSONObject("entity");

                    String orderId = entity.getString("order_id");
                    String paymentId = entity.getString("id");

                    Optional<PaymentOrder> orderOpt = paymentRepository.findByOrderId(orderId);
                    if (orderOpt.isPresent()) {
                        PaymentOrder order = orderOpt.get();
                        if (!"PAID".equals(order.getStatus())) {
                            order.setPaymentId(paymentId);
                            order.setStatus("PAID");
                            paymentRepository.save(order);

                            // Grant course access via webhook
                            grantAccess(order.getUserId(), order.getCourseId(), order.getPaymentId());

                            logger.info("Webhook: Order {} marked as PAID and access granted", orderId);
                        }
                    } else {
                        logger.warn("Webhook: Order {} not found in database", orderId);
                    }
                }
            } else {
                logger.error("Webhook Signature Verification Failed");
            }
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
        }
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public List<PaymentOrder> getPaidOrdersByUserId(Long userId) {
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "PAID");
    }

    public PaymentOrder getOrderByOrderId(String orderId) {
        Optional<PaymentOrder> orderOpt = paymentRepository.findByOrderId(orderId);
        if (orderOpt.isPresent()) {
            PaymentOrder order = orderOpt.get();
            if (!"PAID".equalsIgnoreCase(order.getStatus())) {
                // Try to sync with Razorpay
                try {
                    syncOrderStatusWithGateway(order);
                } catch (Exception e) {
                    logger.warn("Failed to sync order status with gateway: {}", e.getMessage());
                }
            }
            return paymentRepository.findByOrderId(orderId).orElse(null);
        }
        return null;
    }

    private void syncOrderStatusWithGateway(PaymentOrder order) {
        try {
            String url = "https://api.razorpay.com/v1/orders/" + order.getOrderId() + "/payments";
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            String auth = keyId + ":" + keySecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject json = new JSONObject(response.getBody());
                if (json.has("items")) {
                    org.json.JSONArray items = json.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject payment = items.getJSONObject(i);
                        String status = payment.getString("status");
                        if ("captured".equals(status)) {
                            logger.info("Found captured payment {} for order {}", payment.getString("id"), order.getOrderId());
                            order.setStatus("PAID");
                            order.setPaymentId(payment.getString("id"));
                            // order.setSignature(payment.optString("signature")); // Signature not available in this API
                            paymentRepository.save(order);
                            
                            // Grant access
                            grantAccess(order.getUserId(), order.getCourseId(), order.getPaymentId());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error syncing with Razorpay: {}", e.getMessage());
        }
    }

    public void grantAccess(Long userId, Long courseId, String paymentId) {
        try {
            logger.info("Granting access to userId: {} for courseId: {}", userId, courseId);

            Optional<UserCourse> accessOpt = userCourseRepository.findByUserIdAndCourseId(userId, courseId);
            UserCourse access;

            if (accessOpt.isPresent()) {
                access = accessOpt.get();
                logger.info("Updating existing access record for userId: {}", userId);
            } else {
                access = new UserCourse();
                access.setUserId(userId);
                access.setCourseId(courseId);
                logger.info("Creating new access record for userId: {}", userId);
            }

            access.setPaymentId(paymentId);
            access.setAccessStatus("ACTIVE");
            userCourseRepository.save(access);

            logger.info("Course access successfully granted/updated");
        } catch (Exception e) {
            logger.error("Failed to grant course access: {}", e.getMessage(), e);
            // We don't throw here to avoid failing the payment verification itself,
            // but in production we might want more robust retry logic.
        }
    }

    public boolean hasAccess(Long userId, Long courseId) {
        return userCourseRepository.existsByUserIdAndCourseIdAndAccessStatus(userId, courseId, "ACTIVE");
    }
}
