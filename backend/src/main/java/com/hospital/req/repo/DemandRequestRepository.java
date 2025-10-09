package com.hospital.req.repo;

import com.hospital.req.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandRequestRepository extends JpaRepository<DemandRequest, Long> {
    List<DemandRequest> findByCreatedByEmployeeIdOrderByCreatedAtDesc(String empId);
    List<DemandRequest> findAllByOrderByCreatedAtDesc();
}

