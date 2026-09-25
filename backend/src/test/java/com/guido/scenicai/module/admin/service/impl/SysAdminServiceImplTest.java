package com.guido.scenicai.module.admin.service.impl;

import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.admin.dto.AdminRegisterDTO;
import com.guido.scenicai.module.admin.dto.AdminResetPasswordDTO;
import com.guido.scenicai.module.admin.entity.SysAdmin;
import com.guido.scenicai.module.admin.mapper.SysAdminMapper;
import com.guido.scenicai.module.admin.mapper.SysLoginLogMapper;
import com.guido.scenicai.module.admin.service.CaptchaService;
import com.guido.scenicai.module.log.mapper.SysLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysAdminServiceImplTest {

    private static final String RECOVERY_TOKEN = "recovery-token";

    @Mock private SysAdminMapper sysAdminMapper;
    @Mock private SysLogMapper sysLogMapper;
    @Mock private SysLoginLogMapper sysLoginLogMapper;
    @Mock private CaptchaService captchaService;

    private BCryptPasswordEncoder passwordEncoder;
    private SysAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new SysAdminServiceImpl(sysAdminMapper, sysLogMapper, sysLoginLogMapper,
                passwordEncoder, captchaService);
        ReflectionTestUtils.setField(service, "resetToken", RECOVERY_TOKEN);
    }

    @Test
    void closesRegistrationOnceAnAdminExistsAndTheCallerIsNotLoggedIn() {
        when(sysAdminMapper.selectCount(null)).thenReturn(1L);

        BizException error = assertThrows(BizException.class, () -> service.register(register("guido-new")));

        assertEquals(403, error.getCode());
        verify(sysAdminMapper, never()).insert(any(SysAdmin.class));
    }

    @Test
    void allowsBootstrapRegistrationOnAnEmptyAdminTable() {
        when(sysAdminMapper.selectCount(null)).thenReturn(0L);

        service.register(register("guido-first"));

        ArgumentCaptor<SysAdmin> captor = ArgumentCaptor.forClass(SysAdmin.class);
        verify(sysAdminMapper).insert(captor.capture());
        assertEquals("guido-first", captor.getValue().getUsername());
        assertEquals("admin", captor.getValue().getRole());
        assertEquals(1, captor.getValue().getStatus());
        assertNotEquals("first-password", captor.getValue().getPassword());
        assertTrue(passwordEncoder.matches("first-password", captor.getValue().getPassword()));
    }

    @Test
    void staysClosedWhenTheUsernameAlreadyExistsAndNoAdminIsLoggedIn() {
        when(sysAdminMapper.selectCount(null)).thenReturn(2L);
        when(sysAdminMapper.selectOne(any())).thenReturn(new SysAdmin());

        BizException error = assertThrows(BizException.class, () -> service.register(register("admin")));

        assertEquals(403, error.getCode());
        verify(sysAdminMapper, never()).insert(any(SysAdmin.class));
    }

    @Test
    void rejectsRegistrationWhenTheCaptchaFails() {
        doThrow(new BizException(400, "captcha error")).when(captchaService).verify("captcha-id", "7HKQ");

        BizException error = assertThrows(BizException.class, () -> service.register(register("guido-new")));

        assertEquals(400, error.getCode());
        verify(sysAdminMapper, never()).insert(any(SysAdmin.class));
    }

    @Test
    void closesPasswordResetWhenNoRecoveryTokenIsConfigured() {
        ReflectionTestUtils.setField(service, "resetToken", "   ");

        BizException error = assertThrows(BizException.class,
                () -> service.resetPassword(resetPassword("admin", RECOVERY_TOKEN)));

        assertEquals(503, error.getCode());
        verify(sysAdminMapper, never()).updateById(any(SysAdmin.class));
    }

    @Test
    void rejectsPasswordResetWithAWrongRecoveryToken() {
        BizException error = assertThrows(BizException.class,
                () -> service.resetPassword(resetPassword("admin", "wrong-token")));

        assertEquals(403, error.getCode());
        verify(sysAdminMapper, never()).updateById(any(SysAdmin.class));
    }

    @Test
    void rejectsPasswordResetWhenTheTokenIsMissing() {
        BizException error = assertThrows(BizException.class,
                () -> service.resetPassword(resetPassword("admin", null)));

        assertEquals(403, error.getCode());
        verify(sysAdminMapper, never()).updateById(any(SysAdmin.class));
    }

    @Test
    void resetsThePasswordWhenTheRecoveryTokenMatches() {
        SysAdmin admin = new SysAdmin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("old-password"));
        when(sysAdminMapper.selectOne(any())).thenReturn(admin);

        service.resetPassword(resetPassword("admin", RECOVERY_TOKEN));

        ArgumentCaptor<SysAdmin> captor = ArgumentCaptor.forClass(SysAdmin.class);
        verify(sysAdminMapper).updateById(captor.capture());
        assertTrue(passwordEncoder.matches("new-password", captor.getValue().getPassword()));
    }

    private AdminRegisterDTO register(String username) {
        AdminRegisterDTO dto = new AdminRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("first-password");
        dto.setRealName("Guide Ops");
        dto.setCaptchaId("captcha-id");
        dto.setCaptchaCode("7HKQ");
        return dto;
    }

    private AdminResetPasswordDTO resetPassword(String username, String token) {
        AdminResetPasswordDTO dto = new AdminResetPasswordDTO();
        dto.setUsername(username);
        dto.setNewPassword("new-password");
        dto.setCaptchaId("captcha-id");
        dto.setCaptchaCode("7HKQ");
        dto.setResetToken(token);
        return dto;
    }
}