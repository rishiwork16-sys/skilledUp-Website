package com.skilledup.auth.repository;

import com.skilledup.auth.model.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    Optional<OtpRecord> findByIdentifierAndTypeAndVerifiedFalseAndExpiresAtAfter(
            String identifier,
            OtpRecord.OtpType type,
            Instant now);

    void deleteByIdentifierAndType(String identifier, OtpRecord.OtpType type);
}
