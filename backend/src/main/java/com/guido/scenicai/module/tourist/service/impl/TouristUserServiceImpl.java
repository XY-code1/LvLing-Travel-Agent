package com.guido.scenicai.module.tourist.service.impl;

import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.tourist.dto.TouristProfileUpdateDTO;
import com.guido.scenicai.module.tourist.entity.TouristUser;
import com.guido.scenicai.module.tourist.mapper.TouristUserMapper;
import com.guido.scenicai.module.tourist.service.TouristUserService;
import com.guido.scenicai.module.tourist.vo.TouristProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouristUserServiceImpl implements TouristUserService {

    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> AVATAR_IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "webp");
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final TouristUserMapper touristUserMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public TouristProfileVO getProfile() {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        TouristUser user = touristUserMapper.selectById(touristId);
        if (user == null) {
            throw BizException.notFound("游客不存在");
        }
        return buildProfileVO(user);
    }

    @Override
    @Transactional
    public TouristProfileVO updateProfile(TouristProfileUpdateDTO dto) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        TouristUser user = touristUserMapper.selectById(touristId);
        if (user == null) {
            throw BizException.notFound("游客不存在");
        }

        boolean changed = false;
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
            changed = true;
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
            changed = true;
        }
        if (dto.getInterestTags() != null) {
            user.setInterestTags(dto.getInterestTags());
            changed = true;
        }

        if (changed) {
            touristUserMapper.updateById(user);
            log.info("游客资料更新成功: id={}", touristId);
        }

        return buildProfileVO(user);
    }

    @Override
    @Transactional
    public TouristProfileVO uploadAvatar(MultipartFile file) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        TouristUser user = touristUserMapper.selectById(touristId);
        if (user == null) {
            throw BizException.notFound("游客不存在");
        }

        String avatarUrl = storeAvatar(file);
        user.setAvatar(avatarUrl);
        touristUserMapper.updateById(user);
        log.info("游客头像上传成功: id={}, avatar={}", touristId, avatarUrl);
        return buildProfileVO(user);
    }

    private String storeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择头像图片");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BizException(400, "头像图片不能超过 5MB");
        }
        String extension = detectExtension(file);
        if (!AVATAR_IMAGE_TYPES.contains(extension)) {
            throw new BizException(400, "头像仅支持 jpg、png、webp 图片");
        }

        String day = LocalDate.now().format(DAY_FORMAT);
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Path dir = Path.of(uploadPath, "tourist-avatar", day);
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), file.getBytes());
        } catch (Exception e) {
            throw new BizException(500, "头像保存失败", e);
        }
        return "/files/tourist-avatar/" + day + "/" + fileName;
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
        return "";
    }

    private TouristProfileVO buildProfileVO(TouristUser user) {
        TouristProfileVO vo = new TouristProfileVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setInterestTags(user.getInterestTags());
        vo.setAvatarConfigId(user.getAvatarConfigId());
        return vo;
    }
}
