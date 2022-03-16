package com.rajesh.microservices.core.recommendation;

import com.rajesh.microservices.core.recommendation.persistence.RecommendationEntity;
import com.rajesh.microservices.core.recommendation.persistence.RecommendationRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@DataMongoTest
public class PersistenceTests {

    @Autowired
    private RecommendationRepository repository;

    private RecommendationEntity savedEntity;

    @Before
    public void setupDb() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();

        RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");

        StepVerifier.create(repository.save(entity))
                .expectNextMatches(createdEntity -> {
                    savedEntity = createdEntity;
                    return areRecommendationEqual(entity, savedEntity);
                }).verifyComplete();

    }

    @Test
    public void create() {

        RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
        StepVerifier.create(repository.save(newEntity))
                .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
                .verifyComplete();
        StepVerifier.create(repository.findById(newEntity.getId()))
                .expectNextMatches(foundEntity -> areRecommendationEqual(newEntity, foundEntity))
                .verifyComplete();

        StepVerifier.create(repository.count()).expectNext(2l).verifyComplete();
    }

    @Test
    public void update() {
        savedEntity.setAuthor("a2");
        StepVerifier.create(repository.save(savedEntity))
                .expectNextMatches(updatedEntity -> updatedEntity.getAuthor().equals("a2"))
                .verifyComplete();

        StepVerifier.create(repository.findById(savedEntity.getId()))
                .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 &&
                        foundEntity.getAuthor().equals("a2"))
                .verifyComplete();
    }

    @Test
    public void delete() {
        StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
        StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
    }

    @Test
    public void getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
                .expectNextMatches(foundEntity -> areRecommendationEqual(savedEntity, foundEntity))
                .verifyComplete();
    }

    public void duplicateError() {
        RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
        StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
    }

    @Test
    public void optimisticLockError() {

        // Store the saved entity in two separate entity objects
        RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
        RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();

        // Update the entity using the first entity object
        entity1.setAuthor("a1");
        repository.save(entity1).block();

        // Update the entity using the second entity object.
        // This should fail since the second entity now holds a old version number, i.e.
        // a Optimistic Lock Error
        StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

        // Get the updated entity from the database and verify its new sate
        StepVerifier.create(repository.findById(savedEntity.getId()))
                .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 &&
                        foundEntity.getAuthor().equals("a1"))
                .verifyComplete();
    }

    private boolean areRecommendationEqual(RecommendationEntity expected, RecommendationEntity actual) {
        return expected.getId().equals(actual.getId())
                && expected.getAuthor().equals(actual.getAuthor())
                && (expected.getProductId() == actual.getProductId())
                && (expected.getRecommendationId() == actual.getRecommendationId())
                && (expected.getRating() == actual.getRating())
                && expected.getContent().equals(actual.getContent())
                && (expected.getVersion() == actual.getVersion());
    }

}
