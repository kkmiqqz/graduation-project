import org.junit.Test;
import org.urbcomp.startdb.deserializer.StreamDeserialize;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.StreamSerialize;
import org.urbcomp.startdb.utils.GPSBlockReader;

public class TestTogather {
    private static final int BLOCK_SIZE = 1000;
    private String inputFileName = "src/main/resources/chengdu";
    private String dataName = "Chengdu";

    @Test
    public void testStreamCompressionSingleFile() {
        // 只处理一个子文件，例如 "5.txt"
        String singleFile = inputFileName + "/5.txt";
        System.out.println("开始处理文件: " + singleFile);

        StreamSerialize serializer = new StreamSerialize();
        StreamDeserialize deserializer = new StreamDeserialize();
// 新增统计变量
        long totalTimeOriginalSize = 0;
        long totalTimeCompressedSize = 0;
        long totalLonLatOriginalSize = 0;
        long totalLonLatCompressedSize = 0;

        double totalCompressTime = 0.0;
        double totalDecompressTime = 0.0;
        long totalCompressedSize = 0;
        long totalOriginalSize = 0;
        long pointCount = 0;
        // 新增 UID 统计变量
        long totalUidOriginalSize = 0;
        long totalUidCompressedSize = 0;
        try (GPSBlockReader br = new GPSBlockReader(singleFile, BLOCK_SIZE, "5.txt")) {
            gpsPoint point;
            while ((point = br.nextPoint()) != null) {
                long start = System.nanoTime();
                byte[] compressed = serializer.serialize(point);
                long endCompress = System.nanoTime();

                long startDecompress = endCompress;
                gpsPoint decompressed = deserializer.deserialize(compressed);
                long endDecompress = System.nanoTime();
// 累加各部分统计
                totalTimeOriginalSize += serializer.getCurrentTimeOriginalSize();
                totalTimeCompressedSize += serializer.getCurrentTimeCompressedSize();
                totalLonLatOriginalSize += serializer.getCurrentLonLatOriginalSize();
                totalLonLatCompressedSize += serializer.getCurrentLonLatCompressedSize();
                // 累加 UID 统计
                totalUidOriginalSize += serializer.getCurrentUidOriginalSize();
                totalUidCompressedSize += serializer.getCurrentUidCompressedSize();
                // 验证解压后数据是否一致
                if (!point.equals(decompressed)) {
                    System.out.println("解压错误！");
                }

                double compressTime = (endCompress - start) / 1e6;         // 单位：毫秒
                double decompressTime = (endDecompress - startDecompress) / 1e6;
                long compressedSize = compressed.length;
                long originalSize = 24 + point.getId().getBytes().length;   // 假设time+lon+lat=24字节，加上UID长度

                totalCompressTime += compressTime;
                totalDecompressTime += decompressTime;
                totalCompressedSize += compressedSize;
                totalOriginalSize += originalSize;
                pointCount++;
            }
        } catch (Exception e) {
            throw new RuntimeException("处理文件 " + singleFile + " 时出错", e);
        }

        // 文件处理完毕后输出总体性能信息
        double avgCompressTime = (pointCount > 0) ? totalCompressTime / pointCount : 0;
        double avgDecompressTime = (pointCount > 0) ? totalDecompressTime / pointCount : 0;
        double compressionRatio = (totalOriginalSize > 0) ? (double) totalCompressedSize / totalOriginalSize : 0;

       /* System.out.println("文件处理完毕，共处理 " + pointCount + " 个点。");
        System.out.printf("平均压缩时间: %.6f ms\n", avgCompressTime);
        System.out.printf("平均解压时间: %.6f ms\n", avgDecompressTime);
        System.out.println("压缩比: " + compressionRatio);

        // 结束流程
        System.out.println("整个压缩处理流程已完成。");*/
        // 新增输出各部分压缩贡献
        System.out.println("\n=== 各压缩部分贡献 ===");
        System.out.println("时间字段压缩:");
        System.out.printf("  原始总大小: %d 字节\n", totalTimeOriginalSize);
        System.out.printf("  压缩后总大小: %d 字节\n", totalTimeCompressedSize);
        System.out.printf("  压缩率: %.4f%%\n", (double) totalTimeCompressedSize / totalTimeOriginalSize* 100 );

        System.out.println("经纬度字段压缩:");
        System.out.printf("  原始总大小: %d 字节\n", totalLonLatOriginalSize);
        System.out.printf("  压缩后总大小: %d 字节\n", totalLonLatCompressedSize);
        System.out.printf("  压缩率: %.4f%%\n", (double) totalLonLatCompressedSize / totalLonLatOriginalSize* 100);
        // 新增输出 UID 压缩贡献
        System.out.println("UID字段压缩:");
        System.out.printf("  原始总大小: %d 字节\n", totalUidOriginalSize);
        System.out.printf("  压缩后总大小: %d 字节\n", totalUidCompressedSize);
        System.out.printf("  压缩率: %.4f%%\n", (totalUidOriginalSize == 0) ? 0.0 :
                (double) totalUidCompressedSize / totalUidOriginalSize * 100);
    }
}
