package com.skilledup.notification.repository;

import com.skilledup.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByRecipient(String recipient);

    List<NotificationLog> findTop5ByOrderBySentAtDesc();
}
