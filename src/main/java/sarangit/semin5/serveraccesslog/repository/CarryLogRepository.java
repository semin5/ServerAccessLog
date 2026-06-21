package sarangit.semin5.serveraccesslog.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sarangit.semin5.serveraccesslog.domain.CarryLog;

public interface CarryLogRepository extends JpaRepository<CarryLog, Long> {

    List<CarryLog> findByWorkDateBetweenOrderByWorkDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    @Query("""
            select log
            from CarryLog log
            where log.workDate between :startDate and :endDate
              and (:managerName is null or log.exitGuideName = :managerName)
            order by log.workDate desc, log.id desc
            """)
    List<CarryLog> findByWorkDateBetweenAndManager(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("managerName") String managerName
    );
}
