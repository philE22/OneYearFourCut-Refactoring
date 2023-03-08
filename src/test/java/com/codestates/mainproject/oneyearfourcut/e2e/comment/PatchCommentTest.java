package com.codestates.mainproject.oneyearfourcut.e2e.comment;

import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
import com.codestates.mainproject.oneyearfourcut.domain.comment.dto.CommentRequestDto;
import com.codestates.mainproject.oneyearfourcut.domain.comment.entity.Comment;
import com.codestates.mainproject.oneyearfourcut.domain.comment.repository.CommentRepository;
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

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PatchCommentTest {
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
    private CommentRepository commentRepository;
    @Autowired
    private JwtTokenizer jwtTokenizer;
    @Autowired
    private EntityManager em;


    private Member galleryMember;
    private Member artworkMember;
    private Member commentMember;
    private Gallery savedGallery;
    private Artwork savedArtwork;
    private Comment savedComment;

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
        Comment comment = Comment.builder()
                .content("comment content")
                .gallery(savedGallery)
                .artwork(savedArtwork)
                .member(commentMember)
                .build();
        savedComment = commentRepository.save(comment);
    }

    @AfterEach
    void afterSetUp() {
        commentRepository.deleteAll();
        artworkRepository.deleteAll();
        galleryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @DisplayName("정상적인 댓글 수정은 성공한다")
    @Test
    void successPatchTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content("수정된 댓글")
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );
        //then
        Comment comment = commentRepository.findById(savedComment.getCommentId()).get();
        assertThat(comment.getContent()).isEqualTo("수정된 댓글");
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.galleryId").value(savedGallery.getGalleryId()))
                .andExpect(jsonPath("$.commentList.memberId").value(commentMember.getMemberId()))
                .andExpect(jsonPath("$.commentList.content").value("수정된 댓글"));
    }

    @DisplayName("content가 null인 수정은 실패한다")
    @Test
    void nullContentTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content(null)
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("content가 empty인 수정은 실패한다")
    @Test
    void emptyContentTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content("")
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("content가 blank인 수정은 실패한다")
    @Test
    void blankContentTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content(" ")
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isBadRequest());
    }

    @DisplayName("댓글의 주인이 아니면 수정이 실패한다 (전시회 주인 포함)")
    @Test
    void authorizationTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(galleryMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content("수정된 댓글")
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.UNAUTHORIZED.name()));
    }

    @DisplayName("댓글이 요청한 전시회에 포함되지 않으면 수정이 실패한다")
    @Test
    void galleryPathTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content("수정된 댓글")
                .build());
        Gallery newGallery = galleryRepository.save(Gallery.builder()
                .title("title")
                .content("content")
                .member(artworkMember)
                .status(GalleryStatus.OPEN)
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        newGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.COMMENT_NOT_FOUND_FROM_GALLERY.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.COMMENT_NOT_FOUND_FROM_GALLERY.name()));
    }

    @DisplayName("없는 댓글은 수정이 실패한다")
    @Test
    void noCommentTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);
        String body = gson.toJson(CommentRequestDto.builder()
                .content("수정된 댓글")
                .build());

        //when
        ResultActions actions = mockMvc.perform(
                patch("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        Integer.MAX_VALUE
                )
                        .header("Authorization", jwt)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.COMMENT_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.COMMENT_NOT_FOUND.name()));
    }
}
