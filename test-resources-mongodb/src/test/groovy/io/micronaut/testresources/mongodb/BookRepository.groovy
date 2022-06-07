package io.micronaut.testresources.mongodb

import io.micronaut.data.mongodb.annotation.MongoRepository
import io.micronaut.data.repository.CrudRepository

@MongoRepository
interface BookRepository extends CrudRepository<Book, String> {

}
