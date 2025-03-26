package org.urbcomp.startdb.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CompressionController {

    @Autowired
    private CompressionService compressionService;

    /**
     * 上传文件，调用压缩算法，并返回压缩性能数据以及压缩文件（以 Base64 字符串形式）。
     *
     * @param file 上传的轨迹数据文件（支持 txt、plt、csv、excel）
     * @return 包含性能数据与压缩后文件的 JSON 数据
     */
    @PostMapping("/compress")
    public ResponseEntity<?> compressFile(@RequestParam("file") MultipartFile file) {
        try {
            // 将上传的文件保存到临时文件夹
            String originalFilename = file.getOriginalFilename();
            File tempInput = File.createTempFile("input_", "_" + originalFilename);
            file.transferTo(tempInput);

            // 创建输出文件（临时文件），后缀可根据需要修改
            File tempOutput = File.createTempFile("output_", ".bin");

            // 调用压缩服务进行处理
            CompressionResult result = compressionService.compressFile(tempInput.getAbsolutePath(), tempOutput.getAbsolutePath());

            // 读取压缩后的输出文件内容，并转换为 Base64 字符串（前端可以解码后触发下载）
            byte[] fileBytes = Files.readAllBytes(tempOutput.toPath());
            String base64Output = Base64Utils.encodeToString(fileBytes);

            // 构造返回数据，包含性能指标与压缩后的文件数据
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("compressionResult", result);
            responseBody.put("downloadFileBase64", base64Output);

            // 删除临时文件（根据需要决定是否保留）
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
