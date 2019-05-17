/*
 * Copyright 2018 École des Mines de Saint-Étienne.
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
package fr.emse.ci.sparqlext.api;

import fr.emse.ci.sparqlext.utils.Response;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.websocket.Session;
import org.apache.log4j.Level;

/**
 *
 * @author maxime.lefrancois
 */
public class SessionManager {

    private static final int TOO_MANY = 1000;

    private static final Gson GSON = new Gson();

    private boolean isOpen = true;

    private final Session session;

    private final List<Response> responses = new ArrayList<>();

    private final ScheduledExecutorService service;

    private final ExecutorService executor;

    private Level level = Level.TRACE;

    public SessionManager(final Session session) {
        this.session = session;
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(this::flush, 0, 200, TimeUnit.MILLISECONDS);
        executor = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                (ForkJoinPool pool) -> {
                    final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    worker.setName("session-" + session.getId() + "-" + worker.getPoolIndex());
                    return worker;
                },
                null, true);
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public final String getSessionId() {
        return session.getId();
    }

    synchronized void appendLog(String log) {
        if (!isOpen) {
            return;
        }
        responses.add(Response.log(log));
        sendIfTooMany();
    }

    synchronized void appendResult(String result, Response.TYPE type) {
        if (!isOpen) {
            return;
        }
        responses.add(Response.result(result, type));
        sendIfTooMany();
    }

    private void sendIfTooMany() {
        if (responses.size() > TOO_MANY) {
            flush();
        }
    }

    synchronized void clear() {
        responses.add(Response.clear());
        flush();
    }

    synchronized void flush() {
        if (!isOpen) {
            return;
        }
        if (!responses.isEmpty()) {
            try {
                session.getBasicRemote().sendText(GSON.toJson(responses));
            } catch (Exception ex) {
                System.out.println("Error while sending for session: " + session.getId() + ": " + ex.getMessage() + " " + ex + "\n: " + GSON.toJson(responses));
            } finally {
                responses.clear();
            }
        }
    }

    synchronized void close() {
        if (isOpen) {
            isOpen = false;
            service.shutdownNow();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executor.shutdownNow();
            }
        }
    }

}
