package com.skilledup.payment.repository;

import com.skilledup.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(String orderId);

    Optional<PaymentOrder> findByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);

    List<PaymentOrder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    void deleteByCourseId(Long courseId);
}
