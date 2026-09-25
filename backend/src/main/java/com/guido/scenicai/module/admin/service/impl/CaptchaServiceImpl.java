package com.guido.scenicai.module.admin.service.impl;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.admin.service.CaptchaService;
import com.guido.scenicai.module.admin.vo.CaptchaVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片验证码：服务端用带干扰线的扭曲字符图片，答案只留在服务端 Map，
 * 响应里只有图片与句柄——此前把算式明文写进 SVG，base64 一解码就能拿到答案。
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 排除 0/O/1/I 等易混字符；Hutool 的 RandomGenerator 校验时大小写不敏感。 */
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_COUNT = 4;
    private static final int WIDTH = 150;
    private static final int HEIGHT = 44;
    private static final int INTERFERE_COUNT = 6;
    private static final long TTL_MILLIS = 300_000L;
    private static final int MAX_PENDING = 5_000;

    private record Entry(AbstractCaptcha captcha, long expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public CaptchaVO create() {
        purge();
        AbstractCaptcha captcha = new LineCaptcha(WIDTH, HEIGHT, new RandomGenerator(CHARSET, CODE_COUNT), INTERFERE_COUNT);
        String captchaId = UUID.randomUUID().toString();
        store.put(captchaId, new Entry(captcha, System.currentTimeMillis() + TTL_MILLIS));
        return new CaptchaVO(captchaId, captcha.getImageBase64Data());
    }

    /** 校验后立即作废，同一个验证码无法被反复试探。 */
    @Override
    public void verify(String id, String code) {
        Entry entry = id == null ? null : store.remove(id);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()
                || !StringUtils.hasText(code) || !entry.captcha().verify(code.trim())) {
            throw new BizException(400, "图片验证码错误或已过期");
        }
    }

    /** 清理过期条目，并给 Map 一个上限，避免被刷爆内存。 */
    private void purge() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        if (store.size() >= MAX_PENDING) {
            store.clear();
        }
    }
}