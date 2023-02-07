package com.codestates.mainproject.oneyearfourcut.e2e.artwork;

import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.transaction.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PatchArtworkTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Gson gson;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private GalleryRepository galleryRepository;
    @Autowired
    private ArtworkRepository artworkRepository;
    @Autowired
    private JwtTokenizer jwtTokenizer;
    @MockBean
    private AwsS3Service awsS3Service;

    private Gallery savedGallery;
    private Artwork savedArtwork;
    private Member artworkMember;
    private Member galleryMember;
    private MockMultipartFile image = new MockMultipartFile(
            "image",
            "image.png",
            "image/png",
            "<<image.png>>".getBytes());
    @BeforeEach
    void setup() {
        //회원 등록
        artworkMember = memberRepository.save(Member.builder()
                .nickname("test2")
                .email("test2@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path2")
                .build());
        galleryMember = memberRepository.save(Member.builder()
                .nickname("test3")
                .email("test3@gmail.com")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .profile("/path1")
                .build());

        //전시회 등록
        savedGallery = galleryRepository.save(Gallery.builder()
                .member(galleryMember)
                .title("title")
                .content("content")
                .status(GalleryStatus.OPEN)
                .build());

        //작품 등록
        Artwork artwork = Artwork.builder()
                .title("title")
                .content("content")
                .build();
        artwork.setGallery(savedGallery);
        artwork.setMember(artworkMember);
        artwork.setImagePath("/path/test");

        savedArtwork = artworkRepository.save(artwork);
    }

    @DisplayName("모든 변경이 성공한다.")
    @Test
    void allPatchTest() throws Exception {
        //given
        String artworkMemberJwt = jwtTokenizer.testJwtGenerator(artworkMember);
        given(awsS3Service.uploadFile(any())).willReturn("/path/modified");
        willDoNothing().given(awsS3Service).deleteImage(any());

        //when
        ResultActions actions = mockMvc.perform(
                multipart(HttpMethod.PATCH, "/galleries/{gallery-id}/artworks/{artwork-id}",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .file(image)
                        .param("title", "modified title")
                        .param("content", "modified content")
                        .header("Authorization", artworkMemberJwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.artworkId").value(savedArtwork.getArtworkId()))
                .andExpect(jsonPath("$.title").value("modified title"))
                .andExpect(jsonPath("$.content").value("modified content"))
                .andExpect(jsonPath("$.imagePath").value("/path/modified"));

    }

    @DisplayName("작품 작성자가 아니면 수정이 불가능 하다")
    @Test
    void authTest() throws Exception {
        //given
        String galleryMemberJwt = jwtTokenizer.testJwtGenerator(galleryMember);

        //when
        ResultActions actions = mockMvc.perform(
                multipart(HttpMethod.PATCH, "/galleries/{gallery-id}/artworks/{artwork-id}",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .file(image)
                        .header("Authorization", galleryMemberJwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.UNAUTHORIZED.name()));
    }
}
