/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.core;

/**
 * An exception thrown whenever test resources was expected to resolve
 * a property but couldn't.
 */
public class TestResourcesResolutionException extends RuntimeException {
    public TestResourcesResolutionException(Throwable cause) {
        super(cause);
    }

    public TestResourcesResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestResourcesResolutionException(String message) {
        super(message);
    }

    /**
     * Returns a TestResourcesResolutionException from an exception. If
     * the type of the exception is already TestResourcesResolutionException
     * then the same instance is returned, otherwise the exception is
     * wrapped into TestResourcesResolutionException.
     * @param ex the exception
     * @return a TestResourcesResolutionException
     */
    public static TestResourcesResolutionException wrap(Exception ex) {
        if (ex instanceof TestResourcesResolutionException trre) {
            return trre;
        }
        return new TestResourcesResolutionException(ex);
    }
}
