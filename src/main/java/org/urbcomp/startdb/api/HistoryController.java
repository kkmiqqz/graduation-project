package org.urbcomp.startdb.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @GetMapping("/history")
    public List<HistoryRecord> getHistory(HttpSession session) {
        String username = (String) session.getAttribute("user");
        if (username == null) {
            throw new RuntimeException("未登录");
        }
        return historyService.getHistory(username);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id, HttpSession session) {
        String username = (String) session.getAttribute("user");
        if (username == null) {
            throw new RuntimeException("未登录");
        }
        byte[] fileContent = historyService.getFileContent(id, username);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"compressed.bin\"")
                .body(fileContent);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id, HttpSession session) {
        String username = (String) session.getAttribute("user");
        if (username == null) {
            throw new RuntimeException("未登录");
        }
        historyService.deleteRecord(id, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearAllRecords(HttpSession session) {
        String username = (String) session.getAttribute("user");
        if (username == null) {
            throw new RuntimeException("未登录");
        }
        historyService.clearAllRecords(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check")
    public Map<String, Object> checkLogin(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");
        if (username == null) {
            response.put("isLoggedIn", false);
        } else {
            response.put("isLoggedIn", true);
            response.put("username", username);
        }
        return response;
    }
}