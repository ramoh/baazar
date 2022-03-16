package com.rajesh.microservices.core.recommendation.services;

import com.rajesh.api.core.recommendation.Recommendation;
import com.rajesh.api.core.recommendation.RecommendationService;
import com.rajesh.microservices.core.recommendation.persistence.RecommendationEntity;
import com.rajesh.microservices.core.recommendation.persistence.RecommendationRepository;
import com.rajesh.util.exceptions.InvalidInputException;
import com.rajesh.util.http.ServiceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;

    @Autowired
    public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper,
            ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        if (body.getProductId() < 1)
            throw new InvalidInputException("Invalid productId: " + body.getProductId());

        RecommendationEntity entity = mapper.apiToEntity(body);
        Mono<Recommendation> newEntity = repository.save(entity)
                .log("Created a recommendation with recommendation id:" + body.getRecommendationId())
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()
                                + ", Recommendation Id:" + body.getRecommendationId()))
                .map(e -> mapper.entityToApi(e));

        return newEntity.toProcessor().block();
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1)
            throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId)
                .log("Found the recommendation for product:" + productId)
                .map(e -> mapper.entityToApi(e))
                .map(e -> {
                    e.setServiceAddress(serviceUtil.getServiceAddress());
                    return e;
                });
    }

    @Override
    public void deleteRecommendations(int productId) {
        if (productId < 1)
            throw new InvalidInputException("Invalid productId: " + productId);

        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}",
                productId);
        repository.deleteAll(repository.findByProductId(productId)).toProcessor()
                .block();

    }

}
