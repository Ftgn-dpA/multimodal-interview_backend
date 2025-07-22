package com.example.interview.controller;

import com.example.interview.model.Resume;
import com.example.interview.repository.ResumeRepository;
import com.example.interview.repository.UserRepository;
import com.example.interview.model.User;
import com.example.interview.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    // 你可以改成 application.yml 配置
    private static final String RESUME_ROOT_DIR = "C:/Users/leisure/Desktop/softwarebei/interview-resumes";

    // 获取当前登录用户ID（JWT认证）
    private Long getCurrentUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) throw new RuntimeException("用户不存在");
        return user.getId();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file,
                                        @RequestHeader("Authorization") String authHeader) throws IOException {
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("只支持PDF文件");
        }
        // 获取当前用户名
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        // 构建用户子目录
        File userDir = new File(RESUME_ROOT_DIR + "/" + username + "/");
        if (!userDir.exists()) userDir.mkdirs();

        String filename = "resume_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File(userDir, filename);
        file.transferTo(dest);

        // 数据库存储相对路径（如 user123/resume_xxx.pdf）
        String dbPath = username + "/" + filename;

        Resume resume = new Resume();
        resume.setUserId(getCurrentUserId(authHeader));
        resume.setFilename(dbPath);
        resume.setOriginalName(file.getOriginalFilename());
        resume.setUploadTime(new Timestamp(System.currentTimeMillis()));
        resumeRepository.save(resume);

        return ResponseEntity.ok(Map.of("id", resume.getId(), "filename", dbPath));
    }

    @GetMapping("/list")
    public List<Resume> listResumes(@RequestHeader("Authorization") String authHeader) {
        return resumeRepository.findByUserId(getCurrentUserId(authHeader));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id,
                                                 @RequestHeader("Authorization") String authHeader) throws IOException {
        Resume resume = resumeRepository.findById(id).orElse(null);
        if (resume == null || !resume.getUserId().equals(getCurrentUserId(authHeader))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        File file = new File(RESUME_ROOT_DIR + resume.getFilename());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resume.getOriginalName() + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable Long id,
                                        @RequestHeader("Authorization") String authHeader) {
        Resume resume = resumeRepository.findById(id).orElse(null);
        if (resume == null || !resume.getUserId().equals(getCurrentUserId(authHeader))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        File file = new File(RESUME_ROOT_DIR + resume.getFilename());
        if (file.exists()) file.delete();
        resumeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 