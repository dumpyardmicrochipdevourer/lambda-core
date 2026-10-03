package com.microchip.lambda_core.domain.repo;

import java.util.UUID;

import com.microchip.lambda_core.domain.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStorageRepository extends JpaRepository<UserStorage, UUID> {

    @Query("select coalesce(sum(s.usedBytes + s.reservedBytes), 0) from UserStorage s")
    long sumTaken();

    @Query("select coalesce(sum(s.quotaBytes), 0) from UserStorage s")
    long sumQuota();

    @Modifying
    @Query(value = """
            insert into user_storage (user_id, username, quota_bytes) values (:userId, :username, :quota)
            on conflict (user_id) do update set username = excluded.username
            """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId, @Param("username") String username, @Param("quota") long quota);

    @Modifying
    @Query("""
            update UserStorage s set s.reservedBytes = s.reservedBytes + :bytes
            where s.userId = :userId and s.usedBytes + s.reservedBytes + :bytes <= s.quotaBytes
            """)
    int reserve(@Param("userId") UUID userId, @Param("bytes") long bytes);

    @Modifying
    @Query("""
            update UserStorage s set s.reservedBytes = s.reservedBytes - :bytes, s.usedBytes = s.usedBytes + :bytes
            where s.userId = :userId
            """)
    int commit(@Param("userId") UUID userId, @Param("bytes") long bytes);

    @Modifying
    @Query("update UserStorage s set s.reservedBytes = s.reservedBytes - :bytes where s.userId = :userId")
    int release(@Param("userId") UUID userId, @Param("bytes") long bytes);

    @Modifying
    @Query("update UserStorage s set s.usedBytes = s.usedBytes - :bytes where s.userId = :userId")
    int free(@Param("userId") UUID userId, @Param("bytes") long bytes);
}
