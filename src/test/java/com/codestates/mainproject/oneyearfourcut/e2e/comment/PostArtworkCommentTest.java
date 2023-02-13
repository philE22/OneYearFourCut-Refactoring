package com.codestates.mainproject.oneyearfourcut.e2e.comment;

import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
import com.codestates.mainproject.oneyearfourcut.domain.comment.dto.CommentRequestDto;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostArtworkCommentTest {
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

    private Member galleryMember;
    private Member artworkMember;
    private Member commentMember;
    private Gallery savedGallery;
    private Artwork savedArtwork;
    private String jwt;

    @BeforeEach
    void setUp() {
        galleryMember = memberRepository.save(Member.builder()
                .nickname("gallery Writer")
                .email("gallery@gmail.com")
                .profile("/path/gallery")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        savedGallery = galleryRepository.save(Gallery.builder()
                .title("gallery title")
                .content("gallery content")
                .member(galleryMember)
                .status(GalleryStatus.OPEN)
                .build());

        artworkMember = memberRepository.save(Member.builder()
                .nickname("artwork Writer")
                .email("artwork@gmail.com")
                .profile("/path/artwork")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
        Artwork artwork = Artwork.builder()
                .title("artwork title")
                .content("artwork content")
                .build();
        artwork.setImagePath("/path/artwork");
        artwork.setGallery(savedGallery);
        artwork.setMember(artworkMember);
        savedArtwork = artworkRepository.save(artwork);

        commentMember = memberRepository.save(Member.builder()
                .nickname("comment Writer")
                .email("comment@gmail.com")
                .profile("/path/comment")
                .role(Role.USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    @DisplayName("정상적인 작품 댓글 등록이 성공한다.")
    @Test
    void successArtworkCommentTest() throws Exception {
        //given
        jwt = jwtTokenizer.testJwtGenerator(commentMember);
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content("comment content")
                .build();
        String body = gson.toJson(commentRequestDto);

        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.galleryId").value(savedGallery.getGalleryId()))
                .andExpect(jsonPath("$.commentList.memberId").value(commentMember.getMemberId()))
                .andExpect(jsonPath("$.commentList.content").value("comment content"));
    }

    @DisplayName("내용이 없으면 등록에 실패한다.")
    @Test
    void nullContentTest() throws Exception {
        //given
        jwt = jwtTokenizer.testJwtGenerator(commentMember);
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content(null)
                .build();
        String body = gson.toJson(commentRequestDto);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }
    @DisplayName("내용이 빈칸이면 등록에 실패한다.")
    @Test
    void blankContentTest() throws Exception {
        //given
        jwt = jwtTokenizer.testJwtGenerator(commentMember);
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content(" ")
                .build();
        String body = gson.toJson(commentRequestDto);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }
    @DisplayName("내용이 30자 이상이면 등록에 실패한다.")
    @Test
    void contentSizeTest() throws Exception {
        //given
        jwt = jwtTokenizer.testJwtGenerator(commentMember);
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content("일이삼사오육칠팔구십일이삼사오육칠팔구십일이삼사오육칠팔구십일")
                .build();
        String body = gson.toJson(commentRequestDto);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }
    @DisplayName("JWT가 없으면 실패한다.")
    @Test
    void noJWTTest() throws Exception {
        //given
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content("comment content")
                .build();
        String body = gson.toJson(commentRequestDto);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        savedArtwork.getArtworkId())
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isUnauthorized());
    }
    @DisplayName("없는 작품에 댓글 등록이 안된다.")
    @Test
    void notExistArtworkTest() throws Exception {
        //given
        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .content("comment content")
                .build();
        String body = gson.toJson(commentRequestDto);
        jwt = jwtTokenizer.testJwtGenerator(commentMember);


        //when
        ResultActions actions = mockMvc.perform(
                post("/galleries/{gallery-id}/artworks/{artwork-id}/comments",
                        savedGallery.getGalleryId(),
                        Integer.MAX_VALUE)
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.ARTWORK_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.ARTWORK_NOT_FOUND.name()));

    }

}

