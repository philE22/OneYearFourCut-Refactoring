package com.codestates.mainproject.oneyearfourcut.e2e.gallery;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode;
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

import static com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GetGalleryTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Gson gson;
    @Autowired
    private JwtTokenizer jwtTokenizer;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GalleryRepository galleryRepository;

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
    @AfterEach
    void clear() {
        galleryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @DisplayName("정상적인 조회 요청은 성공한다")
    @Test
    void normalGetGalleryTest() throws Exception {
        //given
        String title = "전시회 제목";
        String content = "전시회 내용";

        Gallery gallery = Gallery.builder()
                .title(title)
                .content(content)
                .status(GalleryStatus.OPEN)
                .member(member)
                .build();

        Gallery savedGallery = galleryRepository.save(gallery);

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}", savedGallery.getGalleryId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.galleryId").value(savedGallery.getGalleryId()))
                .andExpect(jsonPath("$.title").value(savedGallery.getTitle()))
                .andExpect(jsonPath("$.content").value(savedGallery.getContent()))
                .andExpect(jsonPath("$.memberId").value(member.getMemberId()))
                .andExpect(jsonPath("$.profile").value(member.getProfile()));
    }

    @DisplayName("폐관된 전시회는 조회가 안된다.")
    @Test
    void getClosedGalleryTest() throws Exception {
        //given
        Gallery gallery = Gallery.builder()
                .member(member)
                .title("제목")
                .content("내용")
                .status(GalleryStatus.CLOSED)
                .build();
        Gallery savedGallery = galleryRepository.save(gallery);

        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}", savedGallery.getGalleryId())
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(CLOSED_GALLERY.getStatus()))
                .andExpect(jsonPath("$.exception").value(CLOSED_GALLERY.name()));
    }

    @DisplayName("전시회가 존재하지 않으면 조회가 안된다.")
    @Test
    void getNotExistGalleryTest() throws Exception {
        //when
        ResultActions actions = mockMvc.perform(
                get("/galleries/{gallery-id}", 1000L)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(GALLERY_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.exception").value(GALLERY_NOT_FOUND.name()));
    }
}
