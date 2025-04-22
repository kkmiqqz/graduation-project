package org.urbcomp.startdb.api;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "history")
@Data
public class HistoryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "filename")
    private String filename;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "compress_time")
    private LocalDateTime compressTime;

    @Lob
    @Column(name = "compressed_file")
    private byte[] compressedFile;

    public byte[] getCompressedFile() {
        return compressedFile;
    }

    public void setUsername(String username) {
        this.username=username;
    }

    public void setFilename(String originalFilename) {
        this.filename=originalFilename;
    }

    public void setUploadTime(LocalDateTime now) {
        this.uploadTime=now;
    }

    public void setCompressTime(LocalDateTime now) {
        this.compressTime=now;
    }

    public void setCompressedFile(byte[] fileBytes) {
        this.compressedFile=fileBytes;
    }
}