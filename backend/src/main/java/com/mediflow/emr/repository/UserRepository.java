package com.mediflow.emr.repository;

import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
