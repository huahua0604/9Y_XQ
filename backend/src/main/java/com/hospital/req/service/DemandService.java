package com.hospital.req.service;

import com.hospital.req.dto.*;
import com.hospital.req.entity.*;
import com.hospital.req.repo.*;
import com.hospital.req.sms.SmsService;
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
    private final SmsService smsService;

    private User mustUser(String empId) {
        return userRepo.findByEmployeeId(empId).orElseThrow();
    }

    @Transactional
    public DemandRequest createOrSubmit(String empId, DemandCreateDto dto) {
        User u = mustUser(empId);
        String contactPhone = dto.getContactPhone();
        if (contactPhone != null) contactPhone = contactPhone.trim();
        if (contactPhone == null || contactPhone.isBlank()) {
            contactPhone = u.getPhone();
            if (contactPhone != null) contactPhone = contactPhone.trim();
        }
        if (dto.isSubmit() && (contactPhone == null || contactPhone.isBlank())) {
            throw new IllegalArgumentException("提交时必须填写联系人电话，或先在用户资料中完善手机号");
        }
        DemandRequest r = new DemandRequest();
        
        r.setTitle(dto.getTitle());
        r.setCategory(dto.getCategory());
        r.setImpactScope(dto.getImpactScope());
        r.setRelatedSystems(dto.getRelatedSystems());
        r.setContactPhone(contactPhone);
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
    public Attachment uploadAttachment(Long reqId, String empId, MultipartFile file, LocalDateTime occurredAt, String note) throws IOException {
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

        String sha = sha256Hex(dest); // 改为流式

        Attachment att = new Attachment();
        att.setRequest(r);
        att.setUploadedBy(u);
        att.setOriginalFilename(file.getOriginalFilename());
        att.setStoredFilename(stored);
        att.setStoragePath(root.relativize(dest).toString().replace(File.separatorChar, '/'));
        att.setContentType(file.getContentType());
        att.setSize(Files.size(dest));
        att.setSha256(sha);
        att.setUploadedAt(LocalDateTime.now());
        att.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
        attRepo.save(att);
        String historyNote = (note != null && !note.isBlank()) ? note : file.getOriginalFilename();
        addHistory(r, u, HistoryAction.ATTACHMENT_ADDED, historyNote, null, att);
        return att;
    }

    private String sha256Hex(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IOException(e);
        }
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

    public void addHistory(DemandRequest r, User actor, HistoryAction action, String note,
                           ApprovalComment comment, Attachment attachment) {
        RequestHistory h = new RequestHistory();
        h.setRequest(r);
        h.setActor(actor);
        h.setAction(action);
        h.setOccurredAt(LocalDateTime.now());
        h.setNote(note);

        h.setComment(comment);
        h.setAttachment(attachment);

        if (comment != null)    h.setCommentRefId(comment.getId());
        if (attachment != null) h.setAttachmentRefId(attachment.getId());

        histRepo.save(h);
    }

    @Transactional
    public DemandRequest updateTitle(Long id, String actorEmpId, String newTitle) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        if (!actor.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("需要ADMIN角色");
        }
        if (newTitle == null || newTitle.isBlank()) {
            throw new RuntimeException("标题不能为空");
        }
        String trimmed = newTitle.trim();
        if (trimmed.length() > 120) {
            throw new RuntimeException("标题长度不能超过120");
        }
        String old = r.getTitle();
        r.setTitle(trimmed);
        addHistory(r, actor, HistoryAction.EDIT,
                String.format("标题：%s -> %s", old == null ? "" : old, trimmed));
        return r;
    }

    private void ensureOwnerOrAR(User actor, DemandRequest req) {
        boolean isOwner = actor.getEmployeeId().equals(req.getCreatedBy().getEmployeeId());
        boolean isAR = actor.getRoles().contains(Role.ADMIN) || actor.getRoles().contains(Role.REVIEWER);
        if (!isOwner && !isAR) throw new RuntimeException("权限不足");
    }

    private static String payload(String key, String label) {
        String safe = (label == null ? key : label).replace("\"", "\\\"");
        return "{\"key\":\"" + key + "\",\"label\":\"" + safe + "\"}";
    }

    @Transactional
    public DemandRequest markCompleted(Long id, String actorEmpId, String note) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        if (!actor.getRoles().contains(Role.ADMIN) && !actor.getRoles().contains(Role.REVIEWER)) {
            throw new RuntimeException("需要ADMIN或REVIEWER角色");
        }
        if (r.getStatus() != RequestStatus.REVIEW_APPROVED) {
            throw new RuntimeException("尚未完成主任与科室审批，不能标记为已完成");
        }

        r.setStatus(RequestStatus.COMPLETED);
        addHistory(r, actor, HistoryAction.MARK_COMPLETED, note == null ? "" : note);
        return r;
    }

    @Transactional
    public DemandRequest unmarkCompleted(Long id, String actorEmpId, String note) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        if (!actor.getRoles().contains(Role.ADMIN) && !actor.getRoles().contains(Role.REVIEWER)) {
            throw new RuntimeException("需要ADMIN或REVIEWER角色");
        }
        if (r.getStatus() != RequestStatus.COMPLETED) {
            throw new RuntimeException("当前不在已完成状态，无法撤销");
        }

        r.setStatus(RequestStatus.REVIEW_APPROVED);
        addHistory(r, actor, HistoryAction.UNMARK_COMPLETED, note == null ? "" : note);
        return r;
    }

    @Transactional
    public DemandRequest markWeekDone(Long id, String actorEmpId, String key, String label) {
        if (key == null || key.isBlank()) throw new RuntimeException("缺少 key");
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        ensureOwnerOrAR(actor, r);

        addHistory(r, actor, HistoryAction.MARK_WEEK_DONE, payload(key, label));
        return r;
    }

    @Transactional
    public DemandRequest unmarkWeekDone(Long id, String actorEmpId, String key, String label) {
        if (key == null || key.isBlank()) throw new RuntimeException("缺少 key");
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        ensureOwnerOrAR(actor, r);

        addHistory(r, actor, HistoryAction.UNMARK_WEEK_DONE, payload(key, label));
        return r;
    }

    @Transactional
    public DemandRequest markMonthDone(Long id, String actorEmpId, String key, String label) {
        if (key == null || key.isBlank()) throw new RuntimeException("缺少 key");
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        ensureOwnerOrAR(actor, r);

        addHistory(r, actor, HistoryAction.MARK_MONTH_DONE, payload(key, label));
        return r;
    }

    @Transactional
    public DemandRequest unmarkMonthDone(Long id, String actorEmpId, String key, String label) {
        if (key == null || key.isBlank()) throw new RuntimeException("缺少 key");
        DemandRequest r = reqRepo.findById(id).orElseThrow();
        User actor = mustUser(actorEmpId);
        ensureOwnerOrAR(actor, r);

        addHistory(r, actor, HistoryAction.UNMARK_MONTH_DONE, payload(key, label));
        return r;
    }

    @Transactional
    public String sendTemplateSms(Long id, String actorEmpId, String content) {
        DemandRequest r = reqRepo.findById(id).orElseThrow();

        String phone = r.getContactPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("联系电话为空，无法发送短信");
        }

        String msg = content == null ? "" : content.trim();
        if (msg.isBlank()) {
            throw new IllegalArgumentException("短信内容不能为空");
        }
        if (msg.length() > 2000) { // 对应 {业务量信息,2000} 的长度限制
            throw new IllegalArgumentException("短信内容超出长度限制(2000)");
        }

        // 关键：这个模板只有 1 个变量，所以 messageContent 就是变量值本身
        return smsService.notifyPhones(phone, msg);
    }

}
