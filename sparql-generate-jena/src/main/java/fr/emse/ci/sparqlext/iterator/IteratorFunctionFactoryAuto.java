/*
 * Copyright 2016 Ecole des Mines de Saint-Etienne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.emse.ci.sparqlext.iterator;

import org.apache.jena.query.QueryBuildException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Extension factory that instantiates a class each time.
 */
class IteratorFunctionFactoryAuto implements IteratorFunctionFactory {

    static final Logger LOG = LoggerFactory.getLogger(IteratorFunctionFactoryAuto.class);

    Class<?> extClass;

    IteratorFunctionFactoryAuto(Class<?> xClass) {
        extClass = xClass;
    }

    @Override
    public IteratorFunction create(String uri) {
        try {
            return (IteratorFunction) extClass.newInstance();
        } catch (Exception e) {
            LOG.debug("Can't instantiate iterator function"
                    + " for " + uri, e);
            throw new QueryBuildException("Can't instantiate iterator function"
                    + " for " + uri, e);
        }
    }
}
