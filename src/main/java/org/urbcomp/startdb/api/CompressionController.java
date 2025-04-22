package org.urbcomp.startdb.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.urbcomp.startdb.gpsPoint;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api")
public class CompressionController {

    @Autowired
    private CompressionService compressionService;

    @Autowired
    private HistoryRepository historyRepository;

    /**
     * 上传文件，逐点压缩并流式返回解压后的轨迹点，同时返回压缩性能数据和压缩文件。
     * 使用 StreamingResponseBody 实现实时数据推送。
     *
     * @param file    上传的轨迹数据文件（支持 txt、plt、csv、excel）
     * @param session HttpSession，用于获取当前登录用户
     * @return StreamingResponseBody 包含逐点的轨迹数据和最终结果
     */
    @PostMapping(value = "/stream-compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public StreamingResponseBody streamCompressFile(@RequestParam("file") MultipartFile file, HttpSession session) {
        return outputStream -> {
            try {
                // 获取当前登录用户名
                String username = (String) session.getAttribute("user");
                if (username == null) {
                    outputStream.write("{\"error\":\"未登录\"}\n".getBytes());
                    return;
                }

                // 将上传的文件保存到临时文件夹
                String originalFilename = file.getOriginalFilename();
                File tempInput = File.createTempFile("input_", "_" + originalFilename);
                file.transferTo(tempInput);

                // 创建输出文件（临时文件）
                File tempOutput = File.createTempFile("output_", ".bin");

                // 定义回调函数，用于发送解压后的轨迹点
                Consumer<gpsPoint> pointConsumer = point -> {
                    try {
                        String json = String.format(
                                "{\"type\":\"point\",\"latitude\":%f,\"longitude\":%f}\n",
                                point.getLatitude(), point.getLongitude()
                        );
                        outputStream.write(json.getBytes());
                        outputStream.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };

                // 调用压缩服务进行处理
                CompressionResult result = compressionService.compressFile(tempInput.getAbsolutePath(), tempOutput.getAbsolutePath(), pointConsumer);

                // 读取压缩后的输出文件内容，并转换为 Base64 字符串
                byte[] fileBytes = Files.readAllBytes(tempOutput.toPath());
                String base64Output = Base64Utils.encodeToString(fileBytes);

                // 保存历史记录
                HistoryRecord record = new HistoryRecord();
                record.setUsername(username);
                record.setFilename(originalFilename);
                record.setUploadTime(LocalDateTime.now());
                record.setCompressTime(LocalDateTime.now());
                record.setCompressedFile(fileBytes);
                historyRepository.save(record);

                // 构造返回数据，包含性能指标与压缩后的文件数据
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("type", "result");
                responseBody.put("compressionResult", result);
                responseBody.put("downloadFileBase64", base64Output);

                // 发送最终结果
                String jsonResult = new ObjectMapper().writeValueAsString(responseBody) + "\n";
                outputStream.write(jsonResult.getBytes());
                outputStream.flush();

                // 删除临时文件
                tempInput.delete();
                tempOutput.delete();
            } catch (Exception e) {
                e.printStackTrace();
                String error = String.format("{\"error\":\"Compression error: %s\"}\n", e.getMessage());
                outputStream.write(error.getBytes());
            }
        };
    }

    /**
     * 原有的批量压缩接口，保留以兼容现有功能
     */
    @PostMapping("/compress")
    public ResponseEntity<?> compressFile(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            String username = (String) session.getAttribute("user");
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录");
            }

            String originalFilename = file.getOriginalFilename();
            File tempInput = File.createTempFile("input_", "_" + originalFilename);
            file.transferTo(tempInput);

            File tempOutput = File.createTempFile("output_", ".bin");

            CompressionResult result = compressionService.compressFile(tempInput.getAbsolutePath(), tempOutput.getAbsolutePath(), null);

            byte[] fileBytes = Files.readAllBytes(tempOutput.toPath());
            String base64Output = Base64Utils.encodeToString(fileBytes);

            HistoryRecord record = new HistoryRecord();
            record.setUsername(username);
            record.setFilename(originalFilename);
            record.setUploadTime(LocalDateTime.now());
            record.setCompressTime(LocalDateTime.now());
            record.setCompressedFile(fileBytes);
            historyRepository.save(record);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("compressionResult", result);
            responseBody.put("downloadFileBase64", base64Output);

            tempInput.delete();
            tempOutput.delete();

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Compression error: " + e.getMessage());
        }
    }
}