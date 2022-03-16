package com.rajesh.microservices.core.recommendation;

import com.rajesh.api.core.recommendation.Recommendation;
import com.rajesh.microservices.core.recommendation.persistence.RecommendationRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.data.mongodb.port: 0" })
public class RecommendationServiceApplicationTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private RecommendationRepository repository;

    @Before
    public void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    public void getRecommendationsByProductId() {

        int productId = 1;

        postAndVerifyRecommendation(productId, 1, OK);
        postAndVerifyRecommendation(productId, 2, OK);
        postAndVerifyRecommendation(productId, 3, OK);

        assertEquals(3, repository.findByProductId(productId).collectList().block().size());

        getAndVerifyRecommendationsByProductId(productId, OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].recommendationId").isEqualTo(3);
    }

    @Test
    public void duplicateError() {

        int productId = 1;
        int recommendationId = 1;

        postAndVerifyRecommendation(productId, recommendationId, OK)
                .jsonPath("$.productId").isEqualTo(productId)
                .jsonPath("$.recommendationId").isEqualTo(recommendationId);

        assertEquals(1, (long) repository.count().block());

        postAndVerifyRecommendation(productId, recommendationId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Recommendation Id:1");

        assertEquals(1, (long) repository.count().block());
    }

    @Test
    public void deleteRecommendations() {

        int productId = 1;
        int recommendationId = 1;

        postAndVerifyRecommendation(productId, recommendationId, OK);
        assertEquals(1, repository.findByProductId(productId).collectList().block().size());

        deleteAndVerifyRecommendationsByProductId(productId, OK);
        assertEquals(0, repository.findByProductId(productId).collectList().block().size());

        deleteAndVerifyRecommendationsByProductId(productId, OK);
    }

    @Test
    public void getRecommendationsInvalidParameter() {

        getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/recommendation");
    }

    @Test
    public void getRecommendationsNotFound() {

        getAndVerifyRecommendationsByProductId("?productId=113", OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void getRecommendationsInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId,
            HttpStatus expectedStatus) {
        return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery,
            HttpStatus expectedStatus) {
        return client.get()
                .uri("/recommendation" + productIdQuery)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId,
            HttpStatus expectedStatus) {
        Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId,
                recommendationId, "Content " + recommendationId, "SA");
        return client.post()
                .uri("/recommendation")
                .body(Mono.just(recommendation), Recommendation.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(int productId,
            HttpStatus expectedStatus) {
        return client.delete()
                .uri("/recommendation?productId=" + productId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody();
    }

}
