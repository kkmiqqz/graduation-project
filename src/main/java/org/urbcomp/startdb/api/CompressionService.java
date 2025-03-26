package org.urbcomp.startdb.api;

import org.springframework.stereotype.Service;
import org.urbcomp.startdb.deserializer.StreamDeserialize;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.StreamSerialize;
import org.urbcomp.startdb.utils.GPSBlockReader;

import java.io.FileOutputStream;

@Service
public class CompressionService {
    private static final int BLOCK_SIZE = 1000;
    private StreamSerialize serializer = new StreamSerialize();
    private StreamDeserialize deserializer = new StreamDeserialize();

    /**
     * 处理给定输入文件，将压缩后的数据写入输出文件，并统计压缩性能数据。
     *
     * @param inputFilePath  输入文件路径（可为 txt、plt、csv、excel，根据文件名后缀进行判断处理）
     * @param outputFilePath 输出文件路径
     * @return 压缩结果数据
     * @throws Exception 当处理过程中出错时抛出异常
     */
    public CompressionResult compressFile(String inputFilePath, String outputFilePath) throws Exception {
        double totalCompressTime = 0.0;
        double totalDecompressTime = 0.0;
        long totalCompressedSize = 0;
        long totalOriginalSize = 0;
        long pointCount = 0;

        // 此处暂时直接用 GPSBlockReader 读取文件，若需要支持多种格式，可根据文件后缀判断后调用不同解析器
        try (GPSBlockReader br = new GPSBlockReader(inputFilePath, BLOCK_SIZE, "fileId");
             FileOutputStream fos = new FileOutputStream(outputFilePath)) {

            gpsPoint point;
            while ((point = br.nextPoint()) != null) {
                long start = System.nanoTime();
                byte[] compressed = serializer.serialize(point);
                long endCompress = System.nanoTime();

                // 这里调用解压验证正确性（实际使用中可以选择移除解压过程以提高效率）
                gpsPoint decompressed = deserializer.deserialize(compressed);
                if (!point.equals(decompressed)) {
                    throw new RuntimeException("Decompression error for point: " + point);
                }
                fos.write(compressed);

                double compressTime = (endCompress - start) / 1e6; // 转换为毫秒
                double decompressTime = 0.0; // 此处未单独计算解压时间
                long compressedSize = compressed.length;
                // 假设原始数据大小为固定值（可根据实际情况调整）
                long originalSize = 24 + point.getId().getBytes().length;

                totalCompressTime += compressTime;
                totalDecompressTime += decompressTime;
                totalCompressedSize += compressedSize;
                totalOriginalSize += originalSize;
                pointCount++;
            }
        }

        CompressionResult result = new CompressionResult();
        result.setTotalPoints(pointCount);
        result.setAverageCompressTime(pointCount > 0 ? totalCompressTime / pointCount : 0);
        result.setAverageDecompressTime(pointCount > 0 ? totalDecompressTime / pointCount : 0);
        result.setCompressionRatio(pointCount > 0 ? (double) totalCompressedSize / totalOriginalSize : 0);
        return result;
    }
}
