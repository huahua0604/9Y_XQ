package com.hospital.req.service;

import com.hospital.req.dto.*;
import com.hospital.req.entity.*;
import com.hospital.req.repo.*;
import com.hospital.req.storage.StoragePathResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.Optional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRequestRepository reqRepo;
    private final UserRepository userRepo;
    private final AttachmentRepository attRepo;
    private final ApprovalCommentRepository commentRepo;
    private final RequestHistoryRepository histRepo;
    private final StoragePathResolver storagePathResolver;

    private User mustUser(String empId) {
        return userRepo.findByEmployeeId(empId).orElseThrow();
    }

    @Transactional
    public DemandRequest createOrSubmit(String empId, DemandCreateDto dto) {
        User u = mustUser(empId);
        DemandRequest r = new DemandRequest();
        r.setTitle(dto.getTitle());
        r.setCategory(dto.getCategory());
        r.setPriority(dto.getPriority() == null ? Priority.MEDIUM : dto.getPriority());
        r.setDesiredDueDate(dto.getDesiredDueDate());
        r.setBudgetEstimate(dto.getBudgetEstimate() == null ? BigDecimal.ZERO : dto.getBudgetEstimate());
        r.setImpactScope(dto.getImpactScope());
        r.setRelatedSystems(dto.getRelatedSystems());
        r.setContactPhone(dto.getContactPhone());
        r.setLocation(dto.getLocation());
        r.setDescription(dto.getDescription());
        r.setCreatedBy(u);
        r.setCreatedByDept(u.getDepartment());
        r.setCreatedAt(LocalDateTime.now());
        r.setStatus(dto.isSubmit() ? RequestStatus.SUBMITTED : RequestStatus.DRAFT);
        if (dto.isSubmit()) r.setSubmittedAt(LocalDateTime.now());
        r = reqRepo.save(r);

        addHistory(r, u, dto.isSubmit() ? HistoryAction.SUBMIT : HistoryAction.CREATE_DRAFT,
                dto.isSubmit() ? "用户提交" : "用户保存草稿");
        return r;
    }

    @Transactional
    public DemandRequest submit(Long id, String empId) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User u = mustUser(empId);
        if (!r.getCreatedBy().getEmployeeId().equals(empId))
            throw new RuntimeException("仅提交人可提交");
        if (r.getStatus() != RequestStatus.DRAFT)
            throw new RuntimeException("当前状态不可提交");
        r.setStatus(RequestStatus.SUBMITTED);
        r.setSubmittedAt(LocalDateTime.now());
        addHistory(r, u, HistoryAction.SUBMIT, "用户提交");
        return r;
    }

    @Transactional
    public DemandRequest adminApprove(Long id, String adminEmpId, String opinion) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User admin = mustUser(adminEmpId);
        if (!admin.getRoles().contains(Role.ADMIN)) throw new RuntimeException("需要ADMIN角色");
        if (r.getStatus() != RequestStatus.SUBMITTED) throw new RuntimeException("状态必须为 SUBMITTED");
        r.setStatus(RequestStatus.ADMIN_APPROVED);
        r.setAdminReviewedAt(LocalDateTime.now());
        r.setAdminOpinion(opinion);
        addHistory(r, admin, HistoryAction.ADMIN_APPROVE, opinion);
        return r;
    }

    @Transactional
    public DemandRequest adminReject(Long id, String adminEmpId, String opinion) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User admin = mustUser(adminEmpId);
        if (!admin.getRoles().contains(Role.ADMIN)) throw new RuntimeException("需要ADMIN角色");
        if (r.getStatus() != RequestStatus.SUBMITTED) throw new RuntimeException("状态必须为 SUBMITTED");
        r.setStatus(RequestStatus.ADMIN_REJECTED);
        r.setAdminReviewedAt(LocalDateTime.now());
        r.setAdminOpinion(opinion);
        addHistory(r, admin, HistoryAction.ADMIN_REJECT, opinion);
        return r;
    }

    @Transactional
    public DemandRequest reviewerApprove(Long id, String reviewerEmpId, String opinion) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User re = mustUser(reviewerEmpId);
        if (!re.getRoles().contains(Role.REVIEWER)) throw new RuntimeException("需要REVIEWER角色");
        if (r.getStatus() != RequestStatus.ADMIN_APPROVED) throw new RuntimeException("状态必须为 ADMIN_APPROVED");
        r.setStatus(RequestStatus.REVIEW_APPROVED);
        r.setReviewerReviewedAt(LocalDateTime.now());
        r.setReviewerOpinion(opinion);
        addHistory(r, re, HistoryAction.REVIEW_APPROVE, opinion);
        // 可选：进入 COMPLETED
        r.setStatus(RequestStatus.COMPLETED);
        addHistory(r, re, HistoryAction.EDIT, "当前状态:已完成，后续可继续添加评论和附件");
        return r;
    }

    @Transactional
    public DemandRequest reviewerReject(Long id, String reviewerEmpId, String opinion) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User re = mustUser(reviewerEmpId);
        if (!re.getRoles().contains(Role.REVIEWER)) throw new RuntimeException("需要REVIEWER角色");
        if (r.getStatus() != RequestStatus.ADMIN_APPROVED) throw new RuntimeException("状态必须为 ADMIN_APPROVED");
        r.setStatus(RequestStatus.REVIEW_REJECTED);
        r.setReviewerReviewedAt(LocalDateTime.now());
        r.setReviewerOpinion(opinion);
        addHistory(r, re, HistoryAction.REVIEW_REJECT, opinion);
        return r;
    }

    @Transactional
    public ApprovalComment addComment(Long id, String empId, String content, Role authorRole) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User u = mustUser(empId);
        ApprovalComment c = new ApprovalComment();
        c.setRequest(r);
        c.setAuthor(u);
        c.setAuthorRole(authorRole);
        c.setContent(content);
        c.setCreatedAt(LocalDateTime.now());
        commentRepo.save(c);
        addHistory(r, u, HistoryAction.COMMENT_ADDED, content, c, null);
        return c;
    }

    @Transactional
    public Attachment uploadAttachment(Long reqId, String empId, MultipartFile file, LocalDateTime occurredAt) throws IOException {
        DemandRequest r = reqRepo.findById(reqId).orElseThrow();
        User u = mustUser(empId);

        Path root = storagePathResolver.root();
        Path dir = root.resolve("demands").resolve(String.valueOf(r.getId()));
        Files.createDirectories(dir);

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf('.')))
                .orElse("");
        String stored = UUID.randomUUID() + ext;
        Path dest = dir.resolve(stored);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }

        String sha = sha256Hex(dest.toFile());

        Attachment att = new Attachment();
        att.setRequest(r);
        att.setUploadedBy(u);
        att.setOriginalFilename(file.getOriginalFilename());
        att.setStoredFilename(stored);
        att.setStoragePath(root.relativize(dest).toString());
        att.setContentType(file.getContentType());
        att.setSize(Files.size(dest));
        att.setSha256(sha);
        att.setUploadedAt(LocalDateTime.now());
        att.setOccurredAt(occurredAt);
        attRepo.save(att);

        addHistory(r, u, HistoryAction.ATTACHMENT_ADDED, file.getOriginalFilename(), null, att);
        return att;
    }


    private String sha256Hex(File f) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) { throw new IOException(e); }
    }

    @Transactional(readOnly = true)
    public List<DemandRequest> listMine(String empId) {
        return reqRepo.findByCreatedByEmployeeIdOrderByCreatedAtDesc(empId);
    }

    @Transactional(readOnly = true)
    public List<DemandRequest> listAllForRole(User u) {
        if (u.getRoles().contains(Role.ADMIN) || u.getRoles().contains(Role.REVIEWER)) {
            return reqRepo.findAllByOrderByCreatedAtDesc();
        }
        return listMine(u.getEmployeeId());
    }

    @Transactional(readOnly = true)
    public DemandRequest get(Long id) {
        return reqRepo.findById(id).orElseThrow();
    }

    public void addHistory(DemandRequest r, User actor, HistoryAction action, String note) {
        addHistory(r, actor, action, note, null, null);
    }

    public void addHistory(DemandRequest r, User actor, HistoryAction action, String note,ApprovalComment comment, Attachment attachment) {
        RequestHistory h = new RequestHistory();
        h.setRequest(r); h.setActor(actor);
        h.setAction(action); h.setOccurredAt(LocalDateTime.now());
        h.setNote(note);

        h.setComment(comment);
        h.setAttachment(attachment);

        if (comment != null)    h.setCommentRefId(comment.getId());
        if (attachment != null) h.setAttachmentRefId(attachment.getId());

        histRepo.save(h);
    }
}
