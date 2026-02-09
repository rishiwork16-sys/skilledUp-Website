package com.skilledup.payment.controller;

import com.razorpay.RazorpayException;
import com.skilledup.payment.dto.ApiMessage;
import com.skilledup.payment.entity.PaymentOrder;
import com.skilledup.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Payment Service is running"));
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        System.out.println("Received create order request: " + data);
        try {
            boolean hasUserId = data.get("userId") != null;
            boolean hasEmail = data.get("email") != null;

            if ((!hasUserId && !hasEmail) || data.get("courseId") == null || data.get("amount") == null) {
                return ResponseEntity.badRequest()
                        .body("Missing required fields: Needs (userId OR email), courseId, and amount");
            }

            Long userId = null;
            if (data.get("userId") != null) {
                try {
                    userId = Long.parseLong(data.get("userId").toString());
                } catch (NumberFormatException e) {
                    // Ignore, might be string/email
                }
            }

            String email = data.containsKey("email") ? data.get("email").toString() : null;

            // If userId is missing, try to find it in the data map under other keys just in
            // case
            if (userId == null && data.containsKey("sub")) {
                // Potentially handle sub, but we rely on email now
            }

            Long courseId = Long.parseLong(data.get("courseId").toString());
            Double amount = Double.parseDouble(data.get("amount").toString());

            return ResponseEntity.ok(paymentService.createOrder(userId, email, courseId, amount));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid number format in fields");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating order: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        String orderId = data.get("orderId");
        String paymentId = data.get("paymentId");
        String signature = data.get("signature");

        try {
            return ResponseEntity.ok(paymentService.verifyPayment(orderId, paymentId, signature));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Verification details failed: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok("Webhook Processed");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentOrder>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaidOrdersByUserId(userId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentOrder> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getOrderByOrderId(orderId));
    }

    @GetMapping("/access")
    public ResponseEntity<Boolean> checkAccess(@RequestParam Long userId, @RequestParam Long courseId) {
        return ResponseEntity.ok(paymentService.hasAccess(userId, courseId));
    }

    @PostMapping(value = "/callback", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Void> paymentCallback(@RequestParam Map<String, String> data,
            @RequestParam(value = "redirect_url", defaultValue = "http://localhost:5173/dashboard/my-orders") String redirectUrl) {
        try {
            String orderId = data.get("razorpay_order_id");
            String paymentId = data.get("razorpay_payment_id");
            String signature = data.get("razorpay_signature");

            if (orderId != null && paymentId != null && signature != null) {
                paymentService.verifyPayment(orderId, paymentId, signature);
                return ResponseEntity.status(302).header("Location", redirectUrl).build();
            } else {
                return ResponseEntity.status(302).header("Location", redirectUrl + "?error=missing_params").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(302).header("Location", redirectUrl + "?error=" + e.getMessage()).build();
        }
    }
}
