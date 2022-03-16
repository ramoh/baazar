package com.rajesh.microservices.core.review.services;

import java.util.List;

import com.rajesh.api.core.review.Review;
import com.rajesh.api.core.review.ReviewService;
import com.rajesh.microservices.core.review.persistence.ReviewEntity;
import com.rajesh.microservices.core.review.persistence.ReviewRepository;
import com.rajesh.util.exceptions.InvalidInputException;
import com.rajesh.util.http.ServiceUtil;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import java.util.function.Supplier;
import static java.util.logging.Level.FINE;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ReviewRepository repository;
    private final ReviewMapper mapper;

    private final Scheduler scheduler;

    @Autowired
    public ReviewServiceImpl(Scheduler scheduler, ReviewRepository repository, ReviewMapper mapper,
            ServiceUtil serviceUtil) {
        this.scheduler = scheduler;
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Review createReview(Review body) {
        if (body.getProductId() < 1)
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        try {
            ReviewEntity entity = mapper.apiToEntity(body);
            ReviewEntity newEntity = repository.save(entity);

            LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
            return mapper.entityToApi(newEntity);

        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException(
                    "Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
        }
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        if (productId < 1)
            throw new InvalidInputException("Invalid productId: " + productId);

        LOG.info("Will get reviews for product with id={}", productId);
        return asyncFlux(() -> Flux.fromIterable(getByProductId(productId))).log(null, FINE);
    }

    @Override
    public void deleteReviews(int productId) {
        if (productId < 1)
            throw new InvalidInputException("Invalid productId: " + productId);
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));

    }

    protected List<Review> getByProductId(int productId) {
        List<ReviewEntity> entityList = repository.findByProductId(productId);
        List<Review> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));
        LOG.debug("getReviews: response size: {}", list.size());
        return list;
    }

    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }

}
