package sarangit.semin5.serveraccesslog.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import sarangit.semin5.serveraccesslog.domain.AccessLog;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    List<AccessLog> findAllByOrderByVisitedAtDescIdDesc();
}
