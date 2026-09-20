package com.guido.scenicai.module.sos.controller;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.sos.dto.SosCreateDTO;
import com.guido.scenicai.module.sos.service.SosRequestService;
import com.guido.scenicai.module.sos.vo.SosRequestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/tourist/sos") @RequiredArgsConstructor
public class TouristSosController {
    private final SosRequestService service;
    @PostMapping public Result<SosRequestVO> create(@Valid @RequestBody SosCreateDTO dto) { return Result.ok(service.create(dto)); }
    @GetMapping("/{requestNo}") public Result<SosRequestVO> get(@PathVariable String requestNo) { return Result.ok(service.get(requestNo)); }
}
