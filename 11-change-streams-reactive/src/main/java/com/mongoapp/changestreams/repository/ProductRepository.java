package com.mongoapp.changestreams.repository;

import com.mongoapp.changestreams.model.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * Same idea as MongoRepository (module 01), but every method returns a
 * reactive type instead of a blocking one: findById returns Mono<Product>,
 * findAll returns Flux<Product>. Nothing runs until something subscribes
 * to the returned Mono/Flux - the controller methods below do that
 * implicitly by returning them directly to WebFlux.
 */
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
}
