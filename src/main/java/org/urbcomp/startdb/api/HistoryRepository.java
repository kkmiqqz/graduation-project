package org.urbcomp.startdb.api;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<HistoryRecord, Long> {
    List<HistoryRecord> findByUsername(String username);
    HistoryRecord findByIdAndUsername(Long id, String username);
}
