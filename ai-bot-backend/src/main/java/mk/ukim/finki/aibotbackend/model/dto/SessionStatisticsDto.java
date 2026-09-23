package mk.ukim.finki.aibotbackend.model.dto;

public record SessionStatisticsDto(
    long totalPosts,
    long macedonianPosts,
    long postsWithMedia,
    long imagePosts,
    long videoPosts,
    long donatedPosts,
    long totalLikes,
    long totalReposts,
    long totalReplies,
    long totalViews
) {
}
