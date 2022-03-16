package com.rajesh.microservices.composite.product.services;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajesh.api.core.product.Product;
import com.rajesh.api.core.product.ProductService;
import com.rajesh.api.core.recommendation.Recommendation;
import com.rajesh.api.core.recommendation.RecommendationService;
import com.rajesh.api.core.review.Review;
import com.rajesh.api.core.review.ReviewService;
import com.rajesh.util.exceptions.InvalidInputException;
import com.rajesh.util.exceptions.NotFoundException;
import com.rajesh.util.http.HttpErrorInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private WebClient webClient;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductCompositeIntegration(
            WebClient.Builder webClient,
            ObjectMapper mapper,

            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort,

            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int recommendationServicePort,

            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int reviewServicePort) {

        this.webClient = webClient.build();
        this.mapper = mapper;

        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort
                + "/recommendation?productId=";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }

    @Override
    public Product createProduct(Product body) {

        String url = productServiceUrl;
        LOG.debug("Will post a new product to URL:{}", url);

        return webClient.post().uri(url)
                .body(Mono.just(body), Product.class)
                .retrieve()
                .bodyToMono(Product.class)
                .log()
                .onErrorMap(error -> handleException(error))
                .toProcessor()
                .block();

    }

    @Override
    public Mono<Product> getProduct(int productId) {

        String url = productServiceUrl + productId;
        LOG.debug("Will call getProduct API on URL: {}", url);

        return webClient.get()
                .uri(url).retrieve()
                .bodyToMono(Product.class)
                .log()
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));

    }

    @Override
    public void deleteProduct(int productId) {
        String url = productServiceUrl + "/" + productId;
        LOG.debug("Will call the deleteProduct API on URL: {}", url);

        webClient.delete().uri(url).retrieve().toBodilessEntity().log().onErrorMap(error -> handleException(error));

    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {

        String url = recommendationServiceUrl;
        LOG.debug("Will post a new recommendation to URL: {}", url);

        return webClient.post().uri(url)
                .body(Mono.just(body), Recommendation.class)
                .retrieve()
                .bodyToMono(Recommendation.class)
                .log()
                .onErrorMap(error -> handleException(error))
                .toProcessor()
                .block();

    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        String url = recommendationServiceUrl + productId;
        LOG.debug("Will call getRecommendations API on URL: {}", url);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToFlux(Recommendation.class).log()
                .onErrorResume(error -> Flux.empty());

    }

    @Override
    public void deleteRecommendations(int productId) {
        String url = recommendationServiceUrl + "?productId=" + productId;
        LOG.debug("Will call the deleteRecommendations API on URL: {}", url);

        webClient.delete().uri(url).retrieve().toBodilessEntity().log().onErrorMap(error -> handleException(error));

    }

    @Override
    public Review createReview(Review body) {

        String url = reviewServiceUrl;
        LOG.debug("Will post a new review to URL: {}", url);

        return webClient.post().uri(url)
                .body(Mono.just(body), Review.class)
                .retrieve()
                .bodyToMono(Review.class)
                .log()
                .onErrorMap(error -> handleException(error))
                .toProcessor()
                .block();

    }

    @Override
    public Flux<Review> getReviews(int productId) {

        String url = reviewServiceUrl + productId;
        LOG.debug("Will call getReviews API on URL: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Review.class)
                .log()
                .onErrorResume(error -> Flux.empty());
    }

    @Override
    public void deleteReviews(int productId) {
        String url = reviewServiceUrl + "?productId=" + productId;
        LOG.debug("Will call the deleteReviews API on URL: {}", url);

        webClient.delete().uri(url).retrieve().toBodilessEntity().log().onErrorMap(error -> handleException(error));

    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException) ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException iex) {
            return iex.getMessage();
        }
    }

}
