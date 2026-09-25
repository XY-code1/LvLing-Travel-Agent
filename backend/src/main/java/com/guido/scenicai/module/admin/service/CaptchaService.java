package com.guido.scenicai.module.admin.service;

import com.guido.scenicai.module.admin.vo.CaptchaVO;

public interface CaptchaService {
    CaptchaVO create();
    void verify(String id, String code);
}
