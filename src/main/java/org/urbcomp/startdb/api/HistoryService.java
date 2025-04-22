package org.urbcomp.startdb.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    public List<HistoryRecord> getHistory(String username) {
        return historyRepository.findByUsername(username);
    }

    public byte[] getFileContent(Long id, String username) {
        HistoryRecord record = historyRepository.findByIdAndUsername(id, username);
        if (record == null) {
            throw new RuntimeException("文件不存在");
        }
        return record.getCompressedFile();
    }

    public void deleteRecord(Long id, String username) {
        HistoryRecord record = historyRepository.findByIdAndUsername(id, username);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        historyRepository.delete(record);
    }

    public void clearAllRecords(String username) {
        List<HistoryRecord> records = historyRepository.findByUsername(username);
        historyRepository.deleteAll(records);
    }
}