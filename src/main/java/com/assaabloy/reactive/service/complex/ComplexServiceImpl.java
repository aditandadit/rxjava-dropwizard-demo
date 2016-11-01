package com.assaabloy.reactive.service.complex;

import com.assaabloy.reactive.service.slow.Slow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

class ComplexServiceImpl implements ComplexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexServiceImpl.class);
    private static final AtomicInteger COUNT = new AtomicInteger();

    @Override
    public Complex getComplex(final Slow slow) {
        try {
            LOGGER.info("Complex START");
            Thread.sleep(500);
            LOGGER.info("Complex DONE");
            return new Complex("I took 500 ms to fetch. I am number " + COUNT.incrementAndGet(), slow);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Callable<Complex> getComplexCallable(final Slow slow) {
        return () -> getComplex(slow);
    }

    @Override
    public Observable<Complex> observeComplex(final Slow slow) {
        return Observable.create(subscriber -> {
            subscriber.onNext(getComplex(slow));
            subscriber.onCompleted();
        });
    }


}