package com.mediflow.emr.repository;

import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.enums.Provider;
import com.mediflow.emr.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from User e where e.email = :email")
    Optional<User> findByEmailForUpdate(@org.springframework.data.repository.query.Param("email") String email);


    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * Provider ID로 사용자 조회
     */
    Optional<User> findByProviderId(String providerId);

    /**
     * Provider와 Provider ID로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 부서 ID로 사용자 목록 조회
     */
    List<User> findByDepartmentId(Long departmentId);

    /**
     * 역할로 사용자 목록 조회
     */
    List<User> findByRole(Role role);

    /**
     * 부서와 역할로 사용자 목록 조회
     */
    List<User> findByDepartmentIdAndRole(Long departmentId, Role role);
}
