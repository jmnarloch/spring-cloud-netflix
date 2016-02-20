/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.rx;

import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;
import rx.Observable;

import java.util.List;

/**
 * A specialized {@link DeferredResult} that handles {@link Observable} type. It subscribes to the provided
 * {@link Observable} and uses {@link Observable#toList()} to accumulate the results and return it as "single entity".
 * As a result no value is going to produced until the entire {@link Observable} completes processing.
 * This isn't going to work well for long running or potentially infinitely running {@link Observable}, resulting in
 * long response latencies and memory utilisation, while in the same time the connection could be closed on the client
 * side due to connection timeout.
 *
 * In order to handle long running event driven tasks consider using {@link ObservableSseEmitter} instead.
 *
 * @author Jakub Narloch
 * @see DeferredResult
 */
class ObservableDeferredResult<T> extends DeferredResult<List<T>> {

    private static final Object EMPTY_RESULT = new Object();

    private final DeferredResultSubscriber<List<T>> subscriber;

    public ObservableDeferredResult(Observable<T> observable) {
        this(null, EMPTY_RESULT, observable);
    }

    public ObservableDeferredResult(long timeout, Observable<T> observable) {
        this(timeout, EMPTY_RESULT, observable);
    }

    public ObservableDeferredResult(Long timeout, Object timeoutResult, Observable<T> observable) {
        super(timeout, timeoutResult);
        Assert.notNull(observable, "observable can not be null");

        subscriber = new DeferredResultSubscriber<List<T>>(observable.toList(), this);
    }
}
