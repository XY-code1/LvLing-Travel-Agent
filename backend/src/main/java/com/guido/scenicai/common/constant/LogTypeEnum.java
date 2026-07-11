package com.guido.scenicai.common.constant;

/**
 * sys_log 日志类型常量（对照数据库设计 §7.1）。
 */
public final class LogTypeEnum {

    private LogTypeEnum() {}

    /** 登录 */
    public static final String LOGIN = "LOGIN";
    /** 操作 */
    public static final String OPERATION = "OPERATION";
    /** 文本/语音/拍照问答 */
    public static final String CHAT = "CHAT";
    /** ASR 语音识别 */
    public static final String ASR = "ASR";
    /** TTS 语音合成 */
    public static final String TTS = "TTS";
    /** 数字人 */
    public static final String AVATAR = "AVATAR";
    /** 多模态/视觉识别 */
    public static final String VISION = "VISION";
    /** 本地 RAG 知识库 */
    public static final String KB = "KB";
    /** 异常/错误 */
    public static final String EXCEPTION = "EXCEPTION";
}
