package com.codestates.mainproject.oneyearfourcut.e2e.gallery;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryPatchDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PatchGalleryTest {
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

    String title = "수정 제목";
    String content = "수정 내용";
    String jwt;
    private Member member;
    private Gallery savedGallery;
    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        savedGallery = galleryRepository.save(Gallery.builder()
                .title("원래 제목")
                .content("원래 내용")
                .member(member)
                .status(GalleryStatus.OPEN)
                .build());
        jwt = jwtTokenizer.testJwtGenerator(member);
    }
    @AfterEach
    void clear() {
        galleryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @DisplayName("제목, 내용 모두 바꾸는 요청에 성공한다")
    @Test
    void successPatchTest() throws Exception {
        //given
        GalleryPatchDto galleryPatchDto = GalleryPatchDto.builder()
                .title(title)
                .content(content)
                .build();
        String body = gson.toJson(galleryPatchDto);

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/me")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        Gallery gallery = galleryRepository.findById(savedGallery.getGalleryId()).get();

        //then
        actions.andExpect(status().isOk());
        assertThat(gallery.getTitle()).isEqualTo(title);
        assertThat(gallery.getContent()).isEqualTo(content);
    }

    @DisplayName("제목만 바꾸는 요청에 성공한다")
    @Test
    void titlePatchTest() throws Exception {
        //given
        GalleryPatchDto galleryPatchDto = GalleryPatchDto.builder()
                .title(title)
                .build();
        String body = gson.toJson(galleryPatchDto);

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/me")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        Gallery gallery = galleryRepository.findById(savedGallery.getGalleryId()).get();
        //then
        actions.andExpect(status().isOk());
        assertThat(gallery.getTitle()).isEqualTo(title);
        assertThat(gallery.getContent()).isEqualTo("원래 내용");
    }

    @DisplayName("내용만 바꾸는 요청에 성공한다")
    @Test
    void contentPatchTest() throws Exception {
        //given
        GalleryPatchDto galleryPatchDto = GalleryPatchDto.builder()
                .content(content)
                .build();
        String body = gson.toJson(galleryPatchDto);

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/me")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        Gallery gallery = galleryRepository.findById(savedGallery.getGalleryId()).get();
        //then
        actions.andExpect(status().isOk());
        assertThat(gallery.getTitle()).isEqualTo("원래 제목");
        assertThat(gallery.getContent()).isEqualTo(content);
    }


    @DisplayName("빈칸으로 바꾸는 요청에 실패한다")
    @Test
    void blankPatchTest() throws Exception {
        //given
        GalleryPatchDto galleryPatchDto = GalleryPatchDto.builder()
                .content(" ")
                .title(" ")
                .build();
        String body = gson.toJson(galleryPatchDto);

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/me")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }















}
