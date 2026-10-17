package com.skyblockm.tweaks.telemetry.sbmtserver.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public record CaptchaBatchUploadDto(
        @NotEmpty(message = "Captchas list cannot be empty")
        @Size(max = 9, message = "Maximum 9 captchas allowed per batch")
        @Schema(description = "Список капч (до 9 штук)")
        List<@Valid CaptchaUploadDto> captchas
) {
    @JsonCreator
    public static CaptchaBatchUploadDto fromJson(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        List<CaptchaUploadDto> list = new ArrayList<>();
        if (node != null) {
            if (node.isArray()) {
                for (JsonNode element : node) {
                    list.add(mapper.convertValue(element, CaptchaUploadDto.class));
                }
            } else if (node.has("captchas") && node.get("captchas").isArray()) {
                for (JsonNode element : node.get("captchas")) {
                    list.add(mapper.convertValue(element, CaptchaUploadDto.class));
                }
            }
        }
        return new CaptchaBatchUploadDto(list);
    }
}
