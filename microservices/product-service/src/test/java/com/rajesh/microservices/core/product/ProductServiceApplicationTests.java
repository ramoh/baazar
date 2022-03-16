package com.rajesh.microservices.core.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rajesh.api.core.product.Product;
import com.rajesh.microservices.core.product.persistence.ProductRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.data.mongodb.port: 0" })
public class ProductServiceApplicationTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductRepository repository;

    @Before
    public void setupDB() {
        this.repository.deleteAll().block();
    }

    @Test
    public void getProductById() {
        int productId = 1;
        postAndVerifyProduct(productId, HttpStatus.OK);
        assertTrue(repository.findByProductId(productId).blockOptional().isPresent());

        getAndVerifyProduct(productId, HttpStatus.OK)
                .jsonPath("$.productId", productId);
    }

    @Test
    public void duplicateError() {
        int productId = 1;
        postAndVerifyProduct(1, HttpStatus.OK);
        assertTrue(repository.findByProductId(1).blockOptional().isPresent());

        postAndVerifyProduct(productId, HttpStatus.UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
    }

    @Test
    public void deleteProduct() {

        int productId = 1;

        postAndVerifyProduct(productId, HttpStatus.OK);
        assertTrue(repository.findByProductId(productId).blockOptional().isPresent());

        deleteAndVerify(productId, HttpStatus.OK);
        assertFalse(repository.findByProductId(productId).blockOptional().isPresent());

        deleteAndVerify(productId, HttpStatus.OK);
    }

    @Test
    public void getProductInvalidParameterString() {
        getAndVerifyProduct("/invalid-id", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/product/invalid-id")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getProductNotFound() {
        int productIdNotFound = 13;
        getAndVerifyProduct(13, HttpStatus.NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No Product found for productId: " + productIdNotFound);
    }

    @Test
    public void getProductInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        getAndVerifyProduct(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        return getAndVerifyProduct("/" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/product" + productIdPath)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        return client.post()
                .uri("/product")
                .body(Mono.just(product), Product.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerify(int productId, HttpStatus expectedStatus) {
        return client.delete()
                .uri("/product/" + productId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody();
    }
}
