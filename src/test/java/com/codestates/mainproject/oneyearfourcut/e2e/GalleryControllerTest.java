package com.codestates.mainproject.oneyearfourcut.e2e;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.controller.GalleryController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
public class GalleryControllerTest {
    @Autowired
    private GalleryController galleryController;
    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("정상적인 전시관 등록시 성공한다")
    void successTest() throws Exception {
        //given

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", "asfd")
        );

        //then
    }
}
