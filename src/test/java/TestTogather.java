import org.junit.Test;
import org.urbcomp.startdb.deserializer.StreamDeserialize;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.StreamSerialize;
import org.urbcomp.startdb.utils.GPSBlockReader;

import java.io.File;

public class TestTogather {
    private static final int BLOCK_SIZE = 1000;
    // private String inputFileName = "src/main/resources/chengdu"; // 数据目录

   // private String inputFileName = "src/main/resources/Geolife"; // 数据目录
     private String inputFileName = "src/main/resources/T-drive"; // 数据目录

    // private String dataName = "Chengdu";

    @Test
    public void testStreamCompression() {
        StreamSerialize serializer = new StreamSerialize();
        StreamDeserialize deserializer = new StreamDeserialize();

        double totalCompressTime = 0.0;
        double totalDecompressTime = 0.0;
        long totalCompressedSize = 0;
        long totalOriginalSize = 0;
        long pointCount = 0;

        File folder = new File(inputFileName);
        File[] files = folder.listFiles();

        if (files == null) {
            System.out.println("file is empty");
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                String filename = file.getName();
                String id = filename; // 使用文件名作为ID
                try (GPSBlockReader br = new GPSBlockReader(inputFileName + "/" + filename, BLOCK_SIZE, id)) {
                    gpsPoint point;
                    while ((point = br.nextPoint()) != null) {
                        long start = System.nanoTime();
                        byte[] compressed = serializer.serialize(point);
                        long endCompress = System.nanoTime();

                        long startDecompress = endCompress;
                        gpsPoint decompressed = deserializer.deserialize(compressed);
                        long endDecompress = System.nanoTime();

                        // 验证
                        if (!point.equals(decompressed)) {
                            System.out.println("Decompression error!");
                        }

                        double compressTime = (endCompress - start) / 1e6;
                        double decompressTime = (endDecompress - startDecompress) / 1e6;
                        long compressedSize = compressed.length;
                        long originalSize = 24 + point.getId().getBytes().length; // 假设time+lon+lat=24字节
                        totalCompressTime += compressTime;
                        totalDecompressTime += decompressTime;
                        totalCompressedSize += compressedSize;
                        totalOriginalSize += originalSize;
                        pointCount++;

                        // 每1000个点打印一次平均性能（这里保持原逻辑）
                        if (pointCount % 1000 == 0) {
                            double avgCompressTime = totalCompressTime / 1000.0;
                            double avgDecompressTime = totalDecompressTime / 1000.0;
                            double compressionRatio = (double) totalCompressedSize / totalOriginalSize;

                            System.out.println("Processed 1000 points:");
                            System.out.printf("Average compression time: %.6f ms\n", avgCompressTime);
                            System.out.printf("Average decompression time: %.6f ms\n", avgDecompressTime);
                            System.out.println("Compression ratio: " + compressionRatio);
                            System.out.println();

                            // 重置计数
                            totalCompressTime = 0.0;
                            totalDecompressTime = 0.0;
                            totalCompressedSize = 0;
                            totalOriginalSize = 0;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(file.getName(), e);
                }
            }
        }
    }
}
