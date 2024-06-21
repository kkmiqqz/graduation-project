import org.junit.Test;
import org.urbcomp.startdb.deserializer.StreamDeserialize;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.StreamSerialize;
import org.urbcomp.startdb.utils.GPSBlockReader;

import java.io.File;

public class TestTogather {
    private static final int BLOCK_SIZE = 1000;
    private String inputFileName = "src/main/resources/chengdu";
    private String dataName = "Chengdu";

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
                        long start = System.currentTimeMillis();
                        byte[] compressed = serializer.serialize(point);
                        long endCompress = System.currentTimeMillis();

                        long startDecompress = endCompress;
                        gpsPoint decompressed = deserializer.deserialize(compressed);
                        long endDecompress = System.currentTimeMillis();

                        // 验证
                        if (!point.equals(decompressed)) {
                            System.out.println("Decompression error!");
                        }

                        long compressTime = endCompress - start;
                        long decompressTime = endDecompress - startDecompress;
                        long compressedSize = compressed.length;
                      //  System.out.println("压缩后大小"+compressedSize);
                     //   System.out.println("UID length: " + point.getId().getBytes().length);
                        long originalSize = 24 + point.getId().getBytes().length; // 假设time+lon+lat=24字节，包含UID长度
                      //  System.out.println("原始大小"+originalSize);
                        totalCompressTime += compressTime;
                        totalDecompressTime += decompressTime;
                        totalCompressedSize += compressedSize;
                        totalOriginalSize += originalSize;
                        pointCount++;

                        // 每1000个点打印一次平均性能
                        if (pointCount % 1000 == 0) {
                            double avgCompressTime = totalCompressTime / 1000.0;
                            double avgDecompressTime = totalDecompressTime / 1000.0;
                            double compressionRatio = (double) totalCompressedSize / totalOriginalSize;

                            System.out.println("Processed 1000 points:");
                            System.out.println("Average compression time: " + avgCompressTime + " ms");
                            System.out.println("Average decompression time: " + avgDecompressTime + " ms");
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