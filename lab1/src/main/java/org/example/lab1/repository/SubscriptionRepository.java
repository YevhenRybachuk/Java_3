package org.example.lab1.repository;

import java.util.List;
import org.example.lab1.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findAllByUserId(Long userId);
}
