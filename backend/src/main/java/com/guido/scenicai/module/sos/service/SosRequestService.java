package com.guido.scenicai.module.sos.service;
import com.guido.scenicai.module.sos.dto.SosCreateDTO;
import com.guido.scenicai.module.sos.vo.SosRequestVO;
public interface SosRequestService { SosRequestVO create(SosCreateDTO dto); SosRequestVO get(String requestNo); }
