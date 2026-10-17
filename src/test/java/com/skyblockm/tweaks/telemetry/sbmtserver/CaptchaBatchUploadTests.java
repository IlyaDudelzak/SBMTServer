package com.skyblockm.tweaks.telemetry.sbmtserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaBatchUploadDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.dto.CaptchaUploadDto;
import com.skyblockm.tweaks.telemetry.sbmtserver.entity.CaptchaClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CaptchaBatchUploadTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private com.skyblockm.tweaks.telemetry.sbmtserver.repository.CaptchaRepository captchaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        captchaRepository.deleteAll();
    }

    private CaptchaUploadDto createDummyCaptcha(byte fillByte) {
        byte[] colors = new byte[16384];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = (byte) ((fillByte * 31 + i) % 256);
        }
        return new CaptchaUploadDto(colors, CaptchaClass.PIGS, 0.5f, false);
    }

    @Test
    void whenUploadingBatchWithObject_shouldSucceed() throws Exception {
        List<CaptchaUploadDto> list = List.of(
                createDummyCaptcha((byte) 10),
                createDummyCaptcha((byte) 20)
        );
        CaptchaBatchUploadDto batchDto = new CaptchaBatchUploadDto(list);

        mockMvc.perform(post("/api/v1/captcha/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void whenUploadingBatchWithRawArray_shouldSucceed() throws Exception {
        List<CaptchaUploadDto> list = List.of(
                createDummyCaptcha((byte) 30)
        );

        mockMvc.perform(post("/api/v1/captcha/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(list)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void whenUploadingMoreThan9Captchas_shouldReturnBadRequest() throws Exception {
        List<CaptchaUploadDto> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(createDummyCaptcha((byte) i));
        }
        CaptchaBatchUploadDto batchDto = new CaptchaBatchUploadDto(list);

        mockMvc.perform(post("/api/v1/captcha/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenUploadingEmptyBatch_shouldReturnBadRequest() throws Exception {
        CaptchaBatchUploadDto batchDto = new CaptchaBatchUploadDto(List.of());

        mockMvc.perform(post("/api/v1/captcha/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDto)))
                .andExpect(status().isBadRequest());
    }
}
