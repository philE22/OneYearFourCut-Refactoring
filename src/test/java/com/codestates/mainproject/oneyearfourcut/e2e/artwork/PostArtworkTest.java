package com.codestates.mainproject.oneyearfourcut.e2e.artwork;

import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.Gallery;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.entity.GalleryStatus;
import com.codestates.mainproject.oneyearfourcut.domain.gallery.repository.GalleryRepository;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Member;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.MemberStatus;
import com.codestates.mainproject.oneyearfourcut.domain.member.entity.Role;
import com.codestates.mainproject.oneyearfourcut.domain.member.repository.MemberRepository;
import com.codestates.mainproject.oneyearfourcut.global.aws.service.AwsS3Service;
import com.codestates.mainproject.oneyearfourcut.global.config.auth.jwt.JwtTokenizer;
import com.codestates.mainproject.oneyearfourcut.global.exception.exception.ExceptionCode;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostArtworkTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Gson gson;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GalleryRepository galleryRepository;
    @Autowired
    private JwtTokenizer jwtTokenizer;
    @MockBean
    private AwsS3Service awsS3Service;

    private Member galleryMember;
    private Member artworkMember;
    private Gallery gallery;
    private String jwt;
    private MockMultipartFile image = new MockMultipartFile(
            "image",
            "image.png",
            "image/png",
            "<<image.png>>".getBytes());

    @BeforeEach
    void beforeSetup() {
        //갤러리 주인 회원과 갤러리 생성
        galleryMember = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        gallery = galleryRepository.save(Gallery.builder()
                .title("gallery title")
                .content("gallery content")
                .member(galleryMember)
                .status(GalleryStatus.OPEN)
                .build());

        //작품 주인 회원과 토큰 생성
        artworkMember = memberRepository.save(Member.builder()
                .nickname("artwork Writer")
                .email("artwork@gmail.com")
                .profile("/path/artwork")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        jwt = jwtTokenizer.testJwtGenerator(artworkMember);

    }

    @DisplayName("정상적인 작품 등록에 성공한다.")
    @Test
    void successPostTest() throws Exception {
        //given
        //s3 업로드 과정 Mock 처리
        given(awsS3Service.uploadFile(any())).willReturn("/savedPath");

        //when
        ResultActions actions = mockMvc.perform(
                multipart("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .file(image)
                        .param("title", "artwork title")
                        .param("content", "artwork content")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isCreated());

    }

    @DisplayName("제목 없이 작품 등록하면 실패한다.")
    @Test
    void noTitlePostTest() throws Exception {
        //given
        //s3 업로드 과정 Mock 처리
        given(awsS3Service.uploadFile(any())).willReturn("/savedPath");

        //when
        ResultActions actions = mockMvc.perform(
                multipart("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .file(image)
                        .param("title", "")
                        .param("content", "artwork content")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("제목을 공백으로 작품 등록하면 실패한다.")
    @Test
    void blankTitlePostTest() throws Exception {
        //given
        //s3 업로드 과정 Mock 처리
        given(awsS3Service.uploadFile(any())).willReturn("/savedPath");

        //when
        ResultActions actions = mockMvc.perform(
                multipart("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .file(image)
                        .param("title", " ")
                        .param("content", "artwork content")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("내용 없이 작품 등록하면 실패한다.")
    @Test
    void noContentPostTest() throws Exception {
        //given
        //s3 업로드 과정 Mock 처리
        given(awsS3Service.uploadFile(any())).willReturn("/savedPath");

        //when
        ResultActions actions = mockMvc.perform(
                multipart("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .file(image)
                        .param("title", "artwork title")
                        .param("content", "")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }


    @DisplayName("내용 공백으로 작품 등록하면 실패한다.")
    @Test
    void blankContentPostTest() throws Exception {
        //given
        //s3 업로드 과정 Mock 처리
        given(awsS3Service.uploadFile(any())).willReturn("/savedPath");

        //when
        ResultActions actions = mockMvc.perform(
                multipart("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .file(image)
                        .param("title", "artwork title")
                        .param("content", " ")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }


    @DisplayName("이미지 없이 작품 등록하면 실패한다.")
    @Test
    void noImagePostTest() throws Exception {
        //given
        //s3 업로드 과정 Mock 처리
        given(awsS3Service.uploadFile(any())).willReturn("/savedPath");

        //when
        ResultActions actions = mockMvc.perform(
                multipart("/galleries/{gallery-id}/artworks", gallery.getGalleryId())
                        .param("title", "artwork title")
                        .param("content", "artwork content")
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.IMAGE_NOT_FOUND_FROM_REQUEST.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.IMAGE_NOT_FOUND_FROM_REQUEST.name()));
    }
}
