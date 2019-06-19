/*
 * Copyright 2019 École des Mines de Saint-Étienne.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author maxime.lefrancois
 */
public final class ExecutionControl {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionControl.class);

    private final CompletableFuture<Void> returnedFuture = new CompletableFuture<>();

    private final List<CompletableFuture<Void>> pendingFutures = Collections.synchronizedList(new ArrayList<>());

    private final CompletableFuture<Void> future = new CompletableFuture<>();

    public ExecutionControl() {
        registerFuture(future);
    }

    void waitFor(CompletableFuture<Void> future) {
        pendingFutures.add(future);
    }

    public void registerFuture(CompletableFuture<Void> newFuture) {
        waitFor(newFuture);
        newFuture.thenRun(() -> stopWaiting(newFuture));
    }

    void stopWaiting(CompletableFuture<Void> completedFuture) {
        if (completedFuture.isCancelled()) {
            returnedFuture.completeExceptionally(new InterruptedException());
        } else if (completedFuture.isCompletedExceptionally()) {
            completedFuture.exceptionally((t) -> {
                returnedFuture.completeExceptionally(t);
                return null;
            });
        } else if (completedFuture.isDone()) {
            pendingFutures.remove(completedFuture);
        }
        if (pendingFutures.isEmpty()) {
            returnedFuture.complete(null);
        }
    }

    public void complete() {
        future.complete(null);
        stopWaiting(future);
    }

    CompletableFuture<Void> getReturnedFuture() {
        return returnedFuture;
    }
}
