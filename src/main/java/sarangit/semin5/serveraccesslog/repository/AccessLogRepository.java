package sarangit.semin5.serveraccesslog.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sarangit.semin5.serveraccesslog.domain.AccessLog;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    List<AccessLog> findAllByOrderByVisitedAtDescIdDesc();

    @Query("""
            select log
            from AccessLog log
            where log.visitedAt between :start and :end
              and (:managerName is null or log.exitGuideName = :managerName)
            order by log.visitedAt desc, log.id desc
            """)
    List<AccessLog> findByVisitedAtBetweenAndManager(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("managerName") String managerName
    );
}
