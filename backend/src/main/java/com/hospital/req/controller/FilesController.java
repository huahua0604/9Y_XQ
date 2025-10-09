package com.hospital.req.controller;

import com.hospital.req.entity.Attachment;
import com.hospital.req.entity.HistoryAction;
import com.hospital.req.repo.AttachmentRepository;
import com.hospital.req.service.DemandService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import com.hospital.req.storage.StoragePathResolver;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FilesController {

    private final AttachmentRepository attRepo;
    private final StoragePathResolver storagePathResolver;
    private final DemandService service;
    private static final Logger log = LoggerFactory.getLogger(FilesController.class);

    // private final String uploadDirRoot = Paths.get("./data/uploads")
    //         .toAbsolutePath().normalize().toString();

    @GetMapping("/{id}/meta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> meta(@PathVariable Long id) {
        Attachment a = attRepo.findById(id).orElseThrow();
        return ResponseEntity.ok(new FileMeta(
                a.getId(), a.getOriginalFilename(), a.getContentType(),
                a.getSize(), a.getUploadedAt(), a.getSha256()
        ));
    }

    @GetMapping(value = "/{id}/inline", produces = MediaType.ALL_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> inline(@PathVariable Long id) {
        return serve(id, /*asAttachment*/ false);
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.ALL_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
        return serve(id, /*asAttachment*/ true);
    }

    private ResponseEntity<FileSystemResource> serve(Long id, boolean asAttachment) {
    Attachment a = attRepo.findById(id).orElseThrow();

    // 如果是软删除且文件已不存在，直接告知客户端 410
    Path resolved = resolveExistingFile(a);
    if (resolved == null) {
        if (a.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.GONE, "文件已删除");
        }
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "文件不存在");
    }

    // content type
    String contentType = a.getContentType();
    try {
        if (contentType == null || contentType.isBlank()) {
            contentType = Files.probeContentType(resolved);
        }
    } catch (Exception ignored) {}
    if (contentType == null || contentType.isBlank()) {
        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    HttpHeaders h = new HttpHeaders();
    h.setContentDisposition(buildContentDisposition(asAttachment, a.getOriginalFilename()));
    h.setContentType(MediaType.parseMediaType(contentType));
    try { h.setContentLength(Files.size(resolved)); } catch (Exception ignored) {}

    return new ResponseEntity<>(new FileSystemResource(resolved), h, HttpStatus.OK);
    }

    private Path resolveExistingFile(Attachment a) {
        Path root = storagePathResolver.root();                   // e.g. {workdir}/data/uploads
        String raw = Optional.ofNullable(a.getStoragePath()).orElse("").replace('\\','/').trim();
        String storedName = Optional.ofNullable(a.getStoredFilename()).orElse("").trim();

        List<Path> candidates = new java.util.ArrayList<>();

        if (!raw.isEmpty()) {
            Path p = Paths.get(raw);
            if (p.isAbsolute()) {
                candidates.add(p.normalize());                    // 老数据：绝对路径
            } else {
                Path base = root.resolve(p).normalize();         // 相对到 root
                candidates.add(base);                             // 假设 raw 已含文件名
                if (!storedName.isEmpty() && !raw.endsWith("/" + storedName) && !raw.equals(storedName)) {
                    candidates.add(base.resolve(storedName).normalize()); // raw 只是目录
                }
            }
        }
        // 兜底：demands/{reqId}/{stored}
        if (!storedName.isEmpty() && a.getRequest() != null && a.getRequest().getId() != null) {
            candidates.add(root.resolve("demands")
                            .resolve(String.valueOf(a.getRequest().getId()))
                            .resolve(storedName).normalize());
        }

        // 安全与存在性检查
        for (Path c : candidates) {
            try {
                // 仅允许 root 下（绝对路径也应位于 root 内，避免越权）
                if (!c.startsWith(root)) {
                    log.warn("Reject path outside root. id={}, path={}, root={}", a.getId(), c, root);
                    continue;
                }
                if (Files.exists(c) && Files.isRegularFile(c) && Files.isReadable(c)) {
                    log.debug("Resolved file for attachment {} -> {}", a.getId(), c);
                    return c;
                }
            } catch (Exception e) {
                log.warn("Check path failed. id={}, path={}, err={}", a.getId(), c, e.toString());
            }
        }

        // 未找到时，打印一下关键信息便于你排查（不会泄露给前端）
        log.warn("File not found. id={}, storagePath='{}', stored='{}', root={}",
                a.getId(), raw, storedName, root);
        return null;
    }



    private static ContentDisposition buildContentDisposition(boolean attachment, String filename) {
        String safe = (filename == null || filename.isBlank()) ? "file" : filename;
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ContentDisposition.builder(attachment ? "attachment" : "inline")
                .filename(safe, StandardCharsets.UTF_8)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        Attachment a = attRepo.findById(id).orElseThrow();
        String me = auth.getName();
        if (a.getUploadedBy() == null || !me.equals(a.getUploadedBy().getEmployeeId())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "只能删除自己上传的附件");
        }

        Path root = storagePathResolver.root();
        String storagePath = Optional.ofNullable(a.getStoragePath()).orElse("").replace('\\','/');
        Path p = storagePath.isBlank() ? Paths.get("") : Paths.get(storagePath);
        String storedName = Optional.ofNullable(a.getStoredFilename()).orElse("").trim();
        boolean storagePathEndsWithStoredName = (!storedName.isEmpty() && storagePath.endsWith("/" + storedName))
                || storagePath.equals(storedName);

        Path filePath;
        if (p.isAbsolute()) {
            filePath = p.normalize();
        } else if (storagePathEndsWithStoredName || storedName.isEmpty()) {
            filePath = root.resolve(p).normalize();
        } else {
            filePath = root.resolve(p).resolve(storedName).normalize();
        }

        try { Files.deleteIfExists(filePath); } catch (Exception ignored) {}

        a.setDeleted(true);
        attRepo.save(a);
        service.addHistory(a.getRequest(), a.getUploadedBy(), HistoryAction.ATTACHMENT_DELETED,a.getOriginalFilename(), null, a);
        return ResponseEntity.noContent().build();
    }

    record FileMeta(Long id, String name, String contentType, long size,
                    java.time.LocalDateTime uploadedAt, String sha256) {}
}
