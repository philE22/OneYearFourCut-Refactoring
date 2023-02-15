package com.codestates.mainproject.oneyearfourcut.e2e.comment;

import com.codestates.mainproject.oneyearfourcut.domain.artwork.entity.Artwork;
import com.codestates.mainproject.oneyearfourcut.domain.artwork.repository.ArtworkRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DeleteCommentTest {
    @Autowired
    private MockMvc mockMvc;
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

    @DisplayName("정상적인 삭제는 성공한다")
    @Test
    void successDeleteTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(commentMember);

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(status().isNoContent());
    }

    @DisplayName("댓글 주인이 아니면 실패한다(작품 주인 포함)")
    @Test
    void authorizationTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(artworkMember);

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.UNAUTHORIZED.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.UNAUTHORIZED.name()));
    }

    @DisplayName("해당 전시회가 아닌 path면 실패한다")
    @Test
    void galleryPathTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(artworkMember);
        Gallery commentMemberGallery = galleryRepository.save(Gallery.builder()
                .title("gallery title")
                .content("gallery content")
                .member(commentMember)
                .status(GalleryStatus.OPEN)
                .build());


        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/{gallery-id}/comments/{comment-id}",
                        commentMemberGallery.getGalleryId(),
                        savedComment.getCommentId()
                )
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.COMMENT_NOT_FOUND_FROM_GALLERY.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.COMMENT_NOT_FOUND_FROM_GALLERY.name()));

    }

    @DisplayName("없는 댓글은 실패한다")
    @Test
    void noCommentTest() throws Exception {
        //given
        String jwt = jwtTokenizer.testJwtGenerator(artworkMember);

        //when
        ResultActions actions = mockMvc.perform(
                delete("/galleries/{gallery-id}/comments/{comment-id}",
                        savedGallery.getGalleryId(),
                        Integer.MAX_VALUE
                )
                        .header("Authorization", jwt)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        //then
        actions.andExpect(jsonPath("$.status").value(ExceptionCode.COMMENT_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.exception").value(ExceptionCode.COMMENT_NOT_FOUND.name()));
    }
}
