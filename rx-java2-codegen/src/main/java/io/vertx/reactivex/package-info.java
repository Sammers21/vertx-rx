/*
 * Copyright (c) 2011-2013 The original author or authors
 *  ------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *      The Eclipse Public License is available at
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 *      The Apache License v2.0 is available at
 *      http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

/**
 * = Vert.x RxJava
 * :toc: left
 *
 * == Vert.x API for RxJava2
 *
 * https://github.com/ReactiveX/RxJava[RxJava] is a popular library for composing asynchronous and event based programs using
 * observable sequences for the Java VM. Vert.x integrates naturally with RxJava, allowing to use
 * observable wherever you can use streams or asynchronous results.
 *
 * === Using Vert.x API for RxJava2
 *
 * To use Vert.x API for RxJava2, add the following dependency to the _dependencies_ section of your build descriptor:
 *
 * * Maven (in your `pom.xml`):
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * ----
 *
 * * Gradle (in your `build.gradle` file):
 *
 * [source,groovy,subs="+attributes"]
 * ----
 * compile '${maven.groupId}:${maven.artifactId}:${maven.version}'
 * ----
 *
 * There are two ways for using the RxJava 2 API with Vert.x:
 *
 * * via the original Vert.x API with helpers class that provides static methods for converting objects between Vert.x core
 * API and RxJava 2 API
 * ** {@link io.vertx.reactivex.RxHelper}
 * ** {@link io.vertx.reactivex.ObservableHelper}
 * ** {@link io.vertx.reactivex.FlowableHelper}
 * ** {@link io.vertx.reactivex.SingleHelper}
 * ** {@link io.vertx.reactivex.MaybeHelper}
 * ** {@link io.vertx.reactivex.CompletableHelper}
 * * via the _Rxified_ Vert.x API enhancing the core Vert.x API.
 *
 * === Read stream support
 *
 * RxJava `Flowable` is a perfect match for Vert.x `ReadStream` class : both provide a flow of items.
 *
 * The {@link io.vertx.reactivex.FlowableHelper#toFlowable(io.vertx.core.streams.ReadStream)} static methods convert
 * a Vert.x read stream to a `Flowable`:
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#toFlowable}
 * ----
 *
 * The _Rxified_ Vert.x API provides a {@link io.vertx.reactivex.core.streams.ReadStream#toFlowable()}  method on
 * {@link io.vertx.reactivex.core.streams.ReadStream}:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#toFlowable}
 * ----
 *
 * Such flowables are *hot* flowables, i.e. they will produce notifications regardless of subscriptions because
 * a `ReadStream` can potentially emit items spontaneously or not, depending on the implementation:
 *
 * At subscription time, the adapter calls {@link io.vertx.core.streams.ReadStream#handler(io.vertx.core.Handler)}
 * to set its own handler.
 *
 * Some `ReadStream` implementations can start to emit events after this call, others will emit events wether an
 * handler is set:
 *
 * - `AsyncFile` produces buffer events after the handler is set
 * - `HttpServerRequest` produces events independantly of the handler (i.e buffer may be lost if no handler is set)
 *
 * In both cases, subscribing to the `Flowable` in the same call is safe because the event loop or the worker
 * verticles cannot be called concurrently, so the subscription will always happens before the handler starts emitting
 * data.
 *
 * When you need to delay the subscription, you need to `pause` the `ReadStream` and then `resume` it, which is what
 * you would do with a `ReadStream`.
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#delayFlowable}
 * ----
 *
 * Likewise it is possible to turn an existing `Flowable` into a Vert.x `ReadStream`.
 *
 * The {@link io.vertx.reactivex.FlowableHelper#toReadStream(io.reactivex.Flowable)}  static methods convert
 * a `Flowable` to a Vert.x read stream:
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#toReadStream}
 * ----
 *
 * === Async result support
 *
 * You can create an RxJava `Observer` from an existing Vert.x `Handler<AsyncResult<T>>` and subscribe
 * it:
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#handlerToSingleObserver}
 * ----
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#handlerToMaybeObserver}
 * ----
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#handlerToCompletableObserver}
 * ----
 *
 * The _Rxified_ Vert.x API duplicates each such method with the `rx` prefix that returns an RxJava {@code Single},
 * {@code Maybe} or {@code Completable}:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#single(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * Such single are *cold* singles, and the corresponding API method is called on subscribe.
 *
 * `Maybe` can produce a result or no result:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#maybe}
 * ----
 *
 * `Completable` is usually mapped to `Handler<AsyncResult<Void>>`
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#completable}
 * ----
 *
 * === Scheduler support
 *
 * The reactive extension sometimes needs to schedule actions, for instance `Flowable#timer` creates and returns
 * a timer that emit periodic events. By default, scheduled actions are managed by RxJava, it means that the
 * timer threads are not Vert.x threads and therefore not executing in a Vert.x event loop nor on a Vert.x worker thread.
 *
 * When an RxJava method deals with a scheduler, it accepts an overloaded method accepting an extra `io.reactivex.Scheduler`,
 * the {@link io.vertx.reactivex.RxHelper#scheduler(io.vertx.core.Vertx)}  method will return a scheduler that can be used
 * in such places.
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#scheduler(io.vertx.core.Vertx)}
 * ----
 *
 * For blocking scheduled actions, a scheduler can be created with the {@link io.vertx.reactivex.RxHelper#blockingScheduler}
 * method:
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#blockingScheduler}
 * ----
 *
 * RxJava can also be reconfigured to use the Vert.x scheduler:
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#schedulerHook(io.vertx.core.Vertx)}
 * ----
 *
 * CAUTION: RxJava uses the words _computation_ for non-blocking tasks and _io_ for blocking tasks
 * which is the opposite of the Vert.x terminology
 *
 * The _Rxified_ Vert.x API provides also similar method on the {@link io.vertx.reactivex.core.RxHelper} class:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#scheduler(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#schedulerHook(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * It is also possible to create a scheduler backed by a named worker pool. This can be useful if you want to re-use
 * the specific thread pool for scheduling blocking actions:
 *
 * [source,$lang]
 * ----
 * {@link examples.RxifiedExamples#scheduler(io.vertx.reactivex.core.WorkerExecutor)}
 * ----
 *
 * === Json unmarshalling
 *
 * The {@link io.vertx.reactivex.FlowableHelper#unmarshaller(Class)} creates an `io.reactivex.rxjava2.FlowableOperator` that
 * transforms an `Flowable<Buffer>` in json format into an object flowable:
 *
 * [source,java]
 * ----
 * {@link examples.NativeExamples#unmarshaller(io.vertx.core.file.FileSystem)}
 * ----
 *
 * The same can be done with the _Rxified_ helper:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#unmarshaller(io.vertx.reactivex.core.file.FileSystem)}
 * ----
 *
 * === Deploying a Verticle
 *
 * To deploy existing Verticle instances, you can use {@link io.vertx.reactivex.core.RxHelper#deployVerticle(io.vertx.reactivex.core.Vertx, io.vertx.core.Verticle)}
 * , it deploys a `Verticle` and returns an `Single<String>` of the deployment ID.
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#deployVerticle}
 * ----
 *
 * = Rxified API
 *
 * The _Rxified_ API is a code generated version of the Vert.x API, just like the _JavaScript_ or _Groovy_
 * language. The API uses the `io.vertx.rxjava` prefix, for instance the `io.vertx.core.Vertx` class is
 * translated to the {@link io.vertx.reactivex.core.Vertx} class.
 *
 * === Embedding Rxfified Vert.x
 *
 * Just use the {@link io.vertx.reactivex.core.Vertx#vertx()} methods:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#embedded()}
 * ----
 *
 * === As a Verticle
 *
 * Extend the {@link io.vertx.reactivex.core.AbstractVerticle} class, it will wrap it for you:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#verticle()}
 * ----
 *
 * Deploying an RxJava verticle is still performed by the Java deployer and does not need a specified
 * deployer.
 *
 * == Api examples
 *
 * Let's study now a few examples of using Vert.x with RxJava.
 *
 * === EventBus message stream
 *
 * The event bus {@link io.vertx.reactivex.core.eventbus.MessageConsumer} provides naturally an `Observable<Message<T>>`:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#eventBusMessages(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * The {@link io.vertx.reactivex.core.eventbus.MessageConsumer} provides a stream of {@link io.vertx.reactivex.core.eventbus.Message}.
 * The {@link io.vertx.reactivex.core.eventbus.Message#body()} gives access to a new stream of message bodies if needed:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#eventBusBodies(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * RxJava map/reduce composition style can then be used:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#eventBusMapReduce(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * === Timers
 *
 * Timer task can be created with {@link io.vertx.reactivex.core.Vertx#timerStream(long)}:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#timer(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * Periodic task can be created with {@link io.vertx.reactivex.core.Vertx#periodicStream(long)}:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#periodic(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * The observable can be cancelled with an unsubscription:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#periodicUnsubscribe(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * === Http client requests
 *
 * We recommend to use the http://vertx.io/docs/vertx-web-client/java/#_rxjava_api[Vert.x Web Client] with RxJava.
 *
 * === Http server requests
 *
 * The {@link io.vertx.reactivex.core.http.HttpServer#requestStream()} provides a callback for each incoming
 * request:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#httpServerRequest}
 * ----
 *
 * The {@link io.vertx.core.http.HttpServerRequest} can then be adapted to an `Observable<Buffer>`:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#httpServerRequestObservable(io.vertx.reactivex.core.http.HttpServer)}
 * ----
 *
 * The {@link io.vertx.reactivex.ObservableHelper#unmarshaller(Class)} can be used to parse and map
 * a json request to an object:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#httpServerRequestObservableUnmarshall(io.vertx.reactivex.core.http.HttpServer)}
 * ----
 *
 * === Websocket client
 *
 * The {@link io.vertx.reactivex.core.http.HttpClient#websocketStream} provides a single callback when the websocket
 * connects, otherwise a failure:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#websocketClient(io.vertx.reactivex.core.Vertx)}
 * ----
 *
 * The {@link io.vertx.reactivex.core.http.WebSocket} can then be turned into an `Observable<Buffer>` easily:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#websocketClientBuffer(io.reactivex.Flowable)}
 * ----
 *
 * === Websocket server
 *
 * The {@link io.vertx.reactivex.core.http.HttpServer#websocketStream()} provides a callback for each incoming
 * connection:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#websocketServer(io.vertx.reactivex.core.http.HttpServer)}
 * ----
 *
 * The {@link io.vertx.core.http.ServerWebSocket} can be turned into an `Observable<Buffer>` easily:
 *
 * [source,java]
 * ----
 * {@link examples.RxifiedExamples#websocketServerBuffer(io.reactivex.Flowable)}
 * ----
 */
@Document(fileName = "index.adoc")
package io.vertx.reactivex;

import io.vertx.docgen.Document;
