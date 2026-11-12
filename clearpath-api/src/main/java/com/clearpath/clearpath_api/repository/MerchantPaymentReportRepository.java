
package com.clearpath.clearpath_api.repository;

import com.clearpath.clearpath_api.entity.MerchantPaymentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantPaymentReportRepository
        extends JpaRepository<MerchantPaymentReport, Long> {

    Optional<MerchantPaymentReport> findByPaypalOrderId(
            String paypalOrderId);
}
