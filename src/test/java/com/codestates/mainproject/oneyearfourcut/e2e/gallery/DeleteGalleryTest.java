package com.codestates.mainproject.oneyearfourcut.e2e.gallery;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DeleteGalleryTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Gson gson;
    @Autowired
    private GalleryRepository galleryRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private JwtTokenizer jwtTokenizer;

    private Member member;
    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    @DisplayName("정상적인 삭제 요청은 성공한다")
    @Test
    void successDeleteTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(member);
        Gallery savedGallery = galleryRepository.save(Gallery.builder()
                .title("원래 제목")
                .content("원래 내용")
                .member(member)
                .status(GalleryStatus.OPEN)
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/me")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isNoContent());
        assertThat(savedGallery.getStatus()).isEqualTo(GalleryStatus.CLOSED);
    }

    @DisplayName("폐쇄된 전시회 삭제는 실패한다.")
    @Test
    void closedDeleteTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(member);
        Gallery savedGallery = galleryRepository.save(Gallery.builder()
                .title("원래 제목")
                .content("원래 내용")
                .member(member)
                .status(GalleryStatus.CLOSED)
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/me")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(GALLERY_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.exception").value(GALLERY_NOT_FOUND.name()));
    }
}
