package sarangit.semin5.serveraccesslog.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sarangit.semin5.serveraccesslog.domain.ItemLog;

public interface ItemLogRepository extends JpaRepository<ItemLog, Long> {
    @Query("""
            select log from ItemLog log
            where (log.receivedDate between :startDate and :endDate
                   or (log.rental = true and log.returnedAt is null))
              and (:itemName is null or log.itemName = :itemName)
              and (:managerName is null or log.managerName = :managerName)
              and (:rental is null or log.rental = :rental)
            order by case when log.rental = true and log.returnedAt is null then 0 else 1 end,
                     log.receivedDate desc, log.id desc
            """)
    List<ItemLog> findForAdmin(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("itemName") String itemName,
                               @Param("managerName") String managerName,
                               @Param("rental") Boolean rental);

    @Query("select distinct log.itemName from ItemLog log order by log.itemName")
    List<String> findDistinctItemNames();
}
