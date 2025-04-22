package org.urbcomp.startdb.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@RestController
public class TrajectoryController {

    @PostMapping("/api/upload")
    public ResponseEntity<?> uploadAndStreamTrajectory(@RequestParam("file") MultipartFile file) {
        try {
            // 保存文件到临时目录
            File tempFile = File.createTempFile("trajectory_", ".tmp");
            file.transferTo(tempFile);

            // 创建 SSE 发射器
            SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 设置超时为无限

            // 启动线程处理文件并发送数据
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 假设每行数据为 "lat,lng" 格式，解压逻辑在这里实现
                        String[] parts = line.split(",");
                        if (parts.length == 2) {
                            // 模拟解压后的经纬度数据
                            String data = String.format("{\"lat\": %s, \"lng\": %s}", parts[0], parts[1]);
                            emitter.send(data); // 发送解压后的数据
                            Thread.sleep(100); // 模拟解压和读取延迟
                        }
                    }
                    emitter.complete(); // 数据发送完成
                } catch (Exception e) {
                    emitter.completeWithError(e); // 处理错误
                } finally {
                    tempFile.delete(); // 删除临时文件
                }
            }).start();

            return ResponseEntity.ok(emitter); // 返回 SSE 发射器
        } catch (Exception e) {
            return ResponseEntity.status(500).body("文件上传失败: " + e.getMessage());
        }
    }
}