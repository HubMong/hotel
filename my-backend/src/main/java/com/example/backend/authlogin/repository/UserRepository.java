package com.example.backend.authlogin.repository;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.domain.User.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    long countByRole(Role role);

    @Query("SELECT u FROM User u WHERE (:name IS NULL OR u.name LIKE CONCAT('%', :name, '%')) " +
           "AND (:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (" +
           "  (:activityStatus IS NULL) OR " +
           "  (:activityStatus = 'ACTIVE' AND (u.lastLoginAt IS NOT NULL AND u.lastLoginAt >= :activeSince)) OR " +
           "  (:activityStatus = 'DORMANT' AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :activeSince))" +
           ")")
    Page<User> findUsersWithFilters(@Param("name") String name,
                                    @Param("email") String email,
                                    @Param("role") Role role,
                                    @Param("activityStatus") String activityStatus,
                                    @Param("activeSince") LocalDateTime activeSince,
                                    Pageable pageable);

    // Overload for simple filtering (merged from admin repository)
    @Query("SELECT u FROM User u WHERE (:name IS NULL OR u.name LIKE CONCAT('%', :name, '%')) " +
           "AND (:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) " +
           "AND (:role IS NULL OR u.role = :role)")
    Page<User> findUsersWithFilters(@Param("name") String name,
                                    @Param("email") String email,
                                    @Param("role") Role role,
                                    Pageable pageable);

    @Query(value = "SELECT u FROM User u WHERE " +
           "(:name IS NULL OR u.name LIKE CONCAT('%', :name, '%')) AND " +
           "(:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "( (:activityStatus IS NULL) OR " +
           "  (:activityStatus = 'ACTIVE' AND u.active = TRUE) OR " +
           "  (:activityStatus = 'DORMANT' AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :activeSince)) ) AND " +
           "(:provider IS NULL OR u.provider = :provider) AND " +
           "(:joinFrom IS NULL OR u.createdOn >= :joinFrom) AND " +
           "( :hasReservation IS NULL OR ( u.role <> :adminRole AND ( " +
           "    ( :hasReservation = 'yes' AND EXISTS (SELECT 1 FROM Reservation r WHERE r.userId = u.id) ) OR " +
           "    ( :hasReservation = 'frequent' AND (SELECT COUNT(r2) FROM Reservation r2 WHERE r2.userId = u.id) >= 3 ) OR " +
           "    ( :hasReservation = 'no' AND NOT EXISTS (SELECT 1 FROM Reservation r3 WHERE r3.userId = u.id) ) " +
           ") ) )",
           countQuery = "SELECT COUNT(u) FROM User u WHERE " +
           "(:name IS NULL OR u.name LIKE CONCAT('%', :name, '%')) AND " +
           "(:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "( (:activityStatus IS NULL) OR " +
           "  (:activityStatus = 'ACTIVE' AND u.active = TRUE) OR " +
           "  (:activityStatus = 'DORMANT' AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :activeSince)) ) AND " +
           "(:provider IS NULL OR u.provider = :provider) AND " +
           "(:joinFrom IS NULL OR u.createdOn >= :joinFrom) AND " +
           "( :hasReservation IS NULL OR ( u.role <> :adminRole AND ( " +
           "    ( :hasReservation = 'yes' AND EXISTS (SELECT 1 FROM Reservation r WHERE r.userId = u.id) ) OR " +
           "    ( :hasReservation = 'frequent' AND (SELECT COUNT(r2) FROM Reservation r2 WHERE r2.userId = u.id) >= 3 ) OR " +
           "    ( :hasReservation = 'no' AND NOT EXISTS (SELECT 1 FROM Reservation r3 WHERE r3.userId = u.id) ) " +
           ") ) )")
    Page<User> findUsersWithAdvancedFilters(@Param("name") String name,
                                            @Param("email") String email,
                                            @Param("role") Role role,
                                            @Param("activityStatus") String activityStatus,
                                            @Param("activeSince") LocalDateTime activeSince,
                                            @Param("provider") User.Provider provider,
                                            @Param("hasReservation") String hasReservation,
                                       @Param("joinFrom") LocalDateTime joinFrom,
                                       @Param("adminRole") Role adminRole,
                                            Pageable pageable);

    @Query("SELECT u.createdOn FROM User u WHERE u.createdOn BETWEEN :start AND :end")
    List<LocalDateTime> findCreationTimesByDateRange(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

       @Query(value = "SELECT MONTH(created_on) AS m, COUNT(*) FROM app_user WHERE YEAR(created_on)=:year GROUP BY m ORDER BY m", nativeQuery = true)
       List<Object[]> countMonthlyUsers(@Param("year") int year);

       @Query(value = "SELECT DATE(created_on) AS d, COUNT(*) FROM app_user WHERE created_on BETWEEN :from AND :to GROUP BY d ORDER BY d", nativeQuery = true)
       List<Object[]> countDailyUsers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
