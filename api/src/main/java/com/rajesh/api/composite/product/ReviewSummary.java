package com.rajesh.api.composite.product;

public class ReviewSummary {

    private final int reviewId;
    private final String author;
    private final String subject;
    private final String content;

    public ReviewSummary() {
        this.reviewId = 0;
        this.author = null;
        this.content = null;
        this.subject = null;
    }

    public ReviewSummary(int reviewId, String author, String subject, String content) {
        this.reviewId = reviewId;
        this.author = author;
        this.subject = subject;
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public int getReviewId() {
        return reviewId;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

}
