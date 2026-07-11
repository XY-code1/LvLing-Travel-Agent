package com.guido.scenicai.module.file.controller;

import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.file.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/image")
    @OperationLog("上传管理端图片")
    public Result<?> uploadImage(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择图片文件");
        }
        String extension = detectExtension(file);
        if (!IMAGE_TYPES.contains(extension)) {
            throw new BizException(400, "仅支持 jpg、png、webp、gif 图片");
        }
        String day = LocalDate.now().format(DAY_FORMAT);
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Path dir = Path.of(uploadPath, "images", day);
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), file.getBytes());
        } catch (Exception e) {
            throw new BizException(500, "图片保存失败", e);
        }
        String url = "/files/images/" + day + "/" + fileName;
        return Result.ok(new FileUploadVO(url, fileName));
    }

    private String detectExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            return original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return "webp";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return "gif";
        }
        return "";
    }
}
