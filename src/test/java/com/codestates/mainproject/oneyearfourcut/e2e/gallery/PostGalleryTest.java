package com.codestates.mainproject.oneyearfourcut.e2e.gallery;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.dto.GalleryRequestDto;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostGalleryTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Gson gson;
    @Autowired
    private JwtTokenizer jwtTokenizer;
    @Autowired
    private  MemberRepository memberRepository;
    @Autowired
    private GalleryRepository galleryRepository;

    @Test
    @DisplayName("정상적인 전시관 등록 시 성공 한다")
    void successTest() throws Exception {
        //given
        Member findMember = memberRepository.findByEmail("test1@gmail.com").get();
        String jwt = jwtTokenizer.testJwtGenerator(findMember);
        String title = "테스트 제목";
        String content = "테스트 내용";

        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title(title)
                .content(content)
                .build();

        String httpContent = gson.toJson(galleryRequestDto);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(httpContent)
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
        Member findMember = memberRepository.findByEmail("test1@gmail.com").get();
        String jwt = jwtTokenizer.testJwtGenerator(findMember);
        GalleryRequestDto galleryRequestDto1 = GalleryRequestDto.builder()
                .title("테스트 제목1")
                .content("테스트 내용1")
                .build();
        String httpContent = gson.toJson(galleryRequestDto1);

        Gallery gallery = Gallery.builder()
                .member(findMember)
                .title("테스트 제목")
                .content("테스트 제목")
                .status(GalleryStatus.OPEN)
                .build();
        galleryRepository.save(gallery);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(httpContent)
        );

        //then
        actions
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.exception").value("OPEN_GALLERY_EXIST"));

    }

    @DisplayName("제목이 없으면 전시관 등록이 실패한다")
    @Test
    void noTitleTest() throws Exception {
        //given
        Member findMember = memberRepository.findByEmail("test1@gmail.com").get();
        String jwt = jwtTokenizer.testJwtGenerator(findMember);

        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .content("테스트 내용")
                .build();

        String httpContent = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(httpContent)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("내용이 없으면 전시관 등록이 실패한다")
    @Test
    void noContentTest() throws Exception {
        //given
        Member findMember = memberRepository.findByEmail("test1@gmail.com").get();
        String jwt = jwtTokenizer.testJwtGenerator(findMember);

        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title("테스트 제목")
                .build();

        String httpContent = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(httpContent)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("내용이 빈칸이면 전시관 등록이 실패한다")
    @Test
    void emptyContentTest() throws Exception {
        //given
        Member findMember = memberRepository.findByEmail("test1@gmail.com").get();
        String jwt = jwtTokenizer.testJwtGenerator(findMember);

        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title("테스트 제목")
                .content("")
                .build();

        String httpContent = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(httpContent)
        );

        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("제목이 빈칸이면 전시관 등록이 실패한다")
    @Test
    void emptyTitleTest() throws Exception {
        //given
        Member findMember = memberRepository.findByEmail("test1@gmail.com").get();
        String jwt = jwtTokenizer.testJwtGenerator(findMember);

        GalleryRequestDto galleryRequestDto = GalleryRequestDto.builder()
                .title("")
                .content("테스트 내용")
                .build();

        String httpContent = gson.toJson(galleryRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries")
                        .header("Authorization", jwt)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(httpContent)
        );

        actions.andExpect(status().isBadRequest());
    }
}











