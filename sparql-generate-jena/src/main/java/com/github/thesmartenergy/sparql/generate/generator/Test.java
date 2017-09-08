/*
 * Copyright 2017 École des Mines de Saint-Étienne.
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
package com.github.thesmartenergy.sparql.generate.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author maxime.lefrancois
 */
public class Test
{
    private final Logger logger = LogManager.getLogger(Test.class);

    public Test(String serialPortName) {
        System.out.println(logger.isInfoEnabled());
        logger.trace("debut");
        logger.debug("debug");
        logger.info("info! {}");
        logger.warn("warb! {}");
        logger.error("error! {}");
    }

    public static void main(String args[])
    {
        Test h1 = new Test("1001");
    }
}