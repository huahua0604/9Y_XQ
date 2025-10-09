package com.hospital.req.controller;

import com.hospital.req.dto.CommentDto;
import com.hospital.req.dto.DemandCreateDto;
import com.hospital.req.dto.DemandDetailDto;
import com.hospital.req.entity.*;
import com.hospital.req.repo.UserRepository;
import com.hospital.req.service.DemandService;
import lombok.RequiredArgsConstructor;

import com.hospital.req.repo.ApprovalCommentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandService service;
    private final UserRepository userRepo;
    private final ApprovalCommentRepository commentRepo;

    private String empId(Authentication auth) { return auth.getName(); }
    private User me(Authentication auth) { return userRepo.findByEmployeeId(empId(auth)).orElseThrow(); }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER','ADMIN','REVIEWER')")
    public DemandDetailDto create(@RequestBody DemandCreateDto dto, Authentication auth) {
        var r = service.createOrSubmit(empId(auth), dto);
        return toDetail(r);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('USER','ADMIN','REVIEWER')")
    public DemandDetailDto submit(@PathVariable Long id, Authentication auth) {
        var r = service.submit(id, empId(auth));
        return toDetail(r);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyAuthority('USER','ADMIN','REVIEWER')")
    public Object mine(Authentication auth) {
        return service.listMine(empId(auth)).stream().map(this::toDetailLite).toList();
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyAuthority('ADMIN','REVIEWER')")
    public Object inbox(Authentication auth) {
        var u = me(auth);
        return service.listAllForRole(u).stream().map(this::toDetailLite).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER','ADMIN','REVIEWER')")
    public DemandDetailDto get(@PathVariable Long id) {
        return toDetail(service.get(id));
    }

    @PostMapping("/{id}/admin/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public DemandDetailDto adminApprove(@PathVariable Long id, @RequestBody CommentDto dto, Authentication auth) {
        var r = service.adminApprove(id, empId(auth), dto.getContent());
        return toDetail(r);
    }

    @PostMapping("/{id}/admin/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public DemandDetailDto adminReject(@PathVariable Long id, @RequestBody CommentDto dto, Authentication auth) {
        var r = service.adminReject(id, empId(auth), dto.getContent());
        return toDetail(r);
    }

    @PostMapping("/{id}/reviewer/approve")
    @PreAuthorize("hasAuthority('REVIEWER')")
    public DemandDetailDto reviewerApprove(@PathVariable Long id, @RequestBody CommentDto dto, Authentication auth) {
        var r = service.reviewerApprove(id, empId(auth), dto.getContent());
        return toDetail(r);
    }

    @PostMapping("/{id}/reviewer/reject")
    @PreAuthorize("hasAuthority('REVIEWER')")
    public DemandDetailDto reviewerReject(@PathVariable Long id, @RequestBody CommentDto dto, Authentication auth) {
        var r = service.reviewerReject(id, empId(auth), dto.getContent());
        return toDetail(r);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyAuthority('USER','ADMIN','REVIEWER')")
    public Object addComment(@PathVariable Long id, @RequestBody CommentDto dto, Authentication auth) {
        Role role = me(auth).getRoles().contains(Role.ADMIN) ? Role.ADMIN :
                (me(auth).getRoles().contains(Role.REVIEWER) ? Role.REVIEWER : Role.USER);
        var c = service.addComment(id, empId(auth), dto.getContent(), role);
        return c.getId();
    }

    @PostMapping(value="/{id}/attachments", consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('USER','ADMIN','REVIEWER')")
    public Object upload(@PathVariable Long id,
                         @RequestPart("file") MultipartFile file,
                         @RequestPart(value="occurredAt", required=false) String occurredAt,
                         Authentication auth) throws Exception {
        var at = service.uploadAttachment(id, empId(auth), file,
                occurredAt == null ? null : LocalDateTime.parse(occurredAt));
        return at.getId();
    }

    private DemandDetailDto toDetailLite(DemandRequest r) {
        DemandDetailDto d = new DemandDetailDto();
        d.setId(r.getId());
        d.setTitle(r.getTitle());
        d.setCategory(r.getCategory());
        d.setPriority(r.getPriority());
        d.setDesiredDueDate(r.getDesiredDueDate());
        d.setBudgetEstimate(r.getBudgetEstimate());
        d.setStatus(r.getStatus());
        d.setCreatedAt(r.getCreatedAt());
        d.setSubmittedAt(r.getSubmittedAt());
        d.setCreatedByEmpId(r.getCreatedBy().getEmployeeId());
        d.setCreatedByName(r.getCreatedBy().getName());
        d.setCreatedByDept(r.getCreatedByDept());
        return d;
    }

    private DemandDetailDto toDetail(DemandRequest r) {
        var d = toDetailLite(r);
        d.setImpactScope(r.getImpactScope());
        d.setRelatedSystems(r.getRelatedSystems());
        d.setContactPhone(r.getContactPhone());
        d.setLocation(r.getLocation());
        d.setDescription(r.getDescription());
        d.setAdminReviewedAt(r.getAdminReviewedAt());
        d.setReviewerReviewedAt(r.getReviewerReviewedAt());
        d.setAdminOpinion(r.getAdminOpinion());
        d.setReviewerOpinion(r.getReviewerOpinion());

        d.setAttachments(r.getAttachments()==null?null:
                r.getAttachments().stream().map(a->{
                    var x = new DemandDetailDto.AttachmentDto();
                    x.setId(a.getId());
                    x.setOriginalFilename(a.getOriginalFilename());
                    x.setContentType(a.getContentType());
                    x.setSize(a.getSize());
                    x.setUploadedAt(a.getUploadedAt());
                    x.setOccurredAt(a.getOccurredAt());
                    // 你的下载接口若尚未实现，可先放占位
                    x.setDownloadUrl("/api/files/" + a.getId() + "/download");
                    if (a.getUploadedBy() != null) {
                        x.setUploadedByEmpId(a.getUploadedBy().getEmployeeId());
                        x.setUploadedByName(a.getUploadedBy().getName());
                    }
                    return x;
                }).collect(Collectors.toList()));

        d.setHistory(r.getHistory()==null?null:
                r.getHistory().stream().map(h->{
                    var x = new DemandDetailDto.HistoryDto();
                    x.setAction(h.getAction().name());
                    x.setActorEmpId(h.getActor().getEmployeeId());
                    x.setActorName(h.getActor().getName());
                    x.setOccurredAt(h.getOccurredAt());
                    x.setNote(h.getNote());
                    x.setCommentId(h.getComment() == null ? null : h.getComment().getId());
                    x.setAttachmentId(h.getAttachment() == null ? null : h.getAttachment().getId());
                    return x;
                }).toList());

        d.setComments(r.getComments()==null?null:
                r.getComments().stream().map(c->{
                    var x = new DemandDetailDto.CommentViewDto();
                    x.setId(c.getId());
                    x.setAuthorEmpId(c.getAuthor().getEmployeeId());
                    x.setAuthorName(c.getAuthor().getName());
                    x.setAuthorRole(c.getAuthorRole().name());
                    x.setContent(c.getContent());
                    x.setCreatedAt(c.getCreatedAt());
                    return x;
                }).toList());

        return d;
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication auth) {
        var c = commentRepo.findById(commentId).orElseThrow();
        String me = empId(auth);

        if (c.getAuthor() == null || !me.equals(c.getAuthor().getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能删除自己发布的评论");
        }
        c.setDeleted(true);
        commentRepo.save(c);
        service.addHistory(c.getRequest(), c.getAuthor(), HistoryAction.COMMENT_DELETED, c.getContent(), c, null);
        return ResponseEntity.noContent().build();
    }

}