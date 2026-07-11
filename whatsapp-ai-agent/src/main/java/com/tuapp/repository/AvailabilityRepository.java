package com.tuapp.repository;

import com.tuapp.model.Availability;
import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByProfessional(Professional professional);

    List<Availability> findByProfessional_Tenant(Tenant tenant);

    void deleteByProfessional_Tenant(Tenant tenant);
}
