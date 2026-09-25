package com.guido.scenicai.module.admin.service.impl;

import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.admin.service.CaptchaService;
import com.guido.scenicai.module.admin.vo.CaptchaVO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CaptchaServiceImpl implements CaptchaService {
    private record Entry(String answer, long expiresAt) {}
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public CaptchaVO create() {
        int a = ThreadLocalRandom.current().nextInt(1, 10);
        int b = ThreadLocalRandom.current().nextInt(1, 10);
        String id = UUID.randomUUID().toString();
        store.put(id, new Entry(String.valueOf(a + b), Instant.now().plusSeconds(300).toEpochMilli()));
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='150' height='48'><rect width='100%' height='100%' rx='6' fill='#f4f7fb'/><text x='75' y='32' text-anchor='middle' font-family='Arial' font-size='24' font-weight='bold' fill='#26354a'>" + a + " + " + b + " = ?</text></svg>";
        String image = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new CaptchaVO(id, image);
    }

    @Override
    public void verify(String id, String code) {
        Entry entry = id == null ? null : store.remove(id);
        if (entry == null || entry.expiresAt < Instant.now().toEpochMilli() || code == null || !entry.answer.equals(code.trim())) {
            throw new BizException(400, "图片验证码错误或已过期");
        }
    }
}
