package io.micronaut.testresources.mongodb

import io.micronaut.context.annotation.Requires
import io.micronaut.data.mongodb.annotation.MongoRepository
import io.micronaut.data.repository.CrudRepository

@MongoRepository(databaseName = "another")
@Requires(env = "multiple-servers")
interface UserRepository extends CrudRepository<User, String> {

}
