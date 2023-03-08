package com.codestates.mainproject.oneyearfourcut.e2e.gallery;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryRequestDto;
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

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import static com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode.OPEN_GALLERY_EXIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
//@Transactional
public class PostGalleryTest {
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

    String jwt;
    Member member;
    String title = "테스트 제목";
    String content = "테스트 내용";

    @BeforeEach
    void setupJWT() {
        member = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        jwt = jwtTokenizer.testJwtGenerator(member);
    }

    @AfterEach
    void afterSetUp() {
        galleryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("정상적인 전시관 등록 시 성공 한다")
    void successTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title(title)
                .content(content)
                .build();
        String body = gson.toJson(galleryRequestDto);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        //then
        actions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content));
    }

    @DisplayName("이미 전시관이 등록된 경우 실패한다")
    @Test
    void alreadyPostTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto1 = GalleryRequestDto.builder()
                .title("테스트 제목1")
                .content("테스트 내용1")
                .build();
        galleryRepository.save(Gallery.builder()
                .status(GalleryStatus.OPEN)
                .member(member)
                .title("title")
                .content("content")
                .build());
        String body = gson.toJson(galleryRequestDto1);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        //then
        actions
                .andExpect(jsonPath("$.status").value(OPEN_GALLERY_EXIST.getStatus()))
                .andExpect(jsonPath("$.exception").value(OPEN_GALLERY_EXIST.name()));

    }

    @DisplayName("제목이 없으면 전시관 등록이 실패한다")
    @Test
    void noTitleTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .content(content)
                .build();
        String body = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("내용이 없으면 전시관 등록이 실패한다")
    @Test
    void noContentTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title(title)
                .build();
        String body = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("내용이 빈칸이면 전시관 등록이 실패한다")
    @Test
    void emptyContentTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title(title)
                .content("")
                .build();
        String body = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("제목이 빈칸이면 전시관 등록이 실패한다")
    @Test
    void emptyTitleTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title("")
                .content(content)
                .build();
        String body = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("제목이 space이면 전시관 등록이 실패한다")
    @Test
    void spaceTitleTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title(" ")
                .content(content)
                .build();
        String body = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("내용이 space이면 전시관 등록이 실패한다")
    @Test
    void spaceContentTest() throws Exception {
        //given
        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title(title)
                .content(" ")
                .build();
        String body = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );

        actions.andExpect(status().isBadRequest());
    }
}











