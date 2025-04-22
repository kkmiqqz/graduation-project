package org.urbcomp.startdb.api;

import org.springframework.stereotype.Service;
import org.urbcomp.startdb.deserializer.StreamDeserialize;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.StreamSerialize;
import org.urbcomp.startdb.utils.GPSBlockReader;

import java.io.FileOutputStream;
import java.util.function.Consumer;

@Service
public class CompressionService {
    private static final int BLOCK_SIZE = 1000;
    private StreamSerialize serializer = new StreamSerialize();
    private StreamDeserialize deserializer = new StreamDeserialize();

    /**
     * 处理给定输入文件，将压缩后的数据写入输出文件，并统计压缩性能数据。
     * 支持逐点回调以发送解压后的轨迹点。
     *
     * @param inputFilePath  输入文件路径（可为 txt、plt、csv、excel）
     * @param outputFilePath 输出文件路径
     * @param pointConsumer  回调函数，用于处理每个解压后的轨迹点（可为 null）
     * @return 压缩结果数据
     * @throws Exception 当处理过程中出错时抛出异常
     */
    public CompressionResult compressFile(String inputFilePath, String outputFilePath, Consumer<gpsPoint> pointConsumer) throws Exception {
        double totalCompressTime = 0.0;
        double totalDecompressTime = 0.0;
        long totalCompressedSize = 0;
        long totalOriginalSize = 0;
        long pointCount = 0;

        try (GPSBlockReader br = new GPSBlockReader(inputFilePath, BLOCK_SIZE, "fileId");
             FileOutputStream fos = new FileOutputStream(outputFilePath)) {

            gpsPoint point;
            while ((point = br.nextPoint()) != null) {
                long start = System.nanoTime();
                byte[] compressed = serializer.serialize(point);
                long endCompress = System.nanoTime();
                long startDecompress = endCompress;
                gpsPoint decompressed = deserializer.deserialize(compressed);
                long endDecompress = System.nanoTime();

                if (!point.equals(decompressed)) {
                    throw new RuntimeException("Decompression error for point: " + point);
                }

                fos.write(compressed);

                if (pointConsumer != null) {
                    pointConsumer.accept(decompressed);
                }

                double compressTime = (endCompress - start) / 1e6; // 转换为毫秒
                double decompressTime = (endDecompress - startDecompress) / 1e6;
                long compressedSize = compressed.length;
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