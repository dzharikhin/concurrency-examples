package tokenholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 20.11.2016.
 */
public class ReadWriteLockTokenResource {

    static volatile boolean wasError = false;
    static AtomicInteger tokenSrc = new AtomicInteger(1);
    static volatile int requestCounter = 0;

    static /*volatile*/ int token;
    static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    static final Lock readLock = readWriteLock.readLock();
    static final Lock writeLock = readWriteLock.writeLock();

    static class ConsumerThread extends Thread {

        private static final Random random = new Random();

        public ConsumerThread(int i) {
            super("Consumer " + i);
        }

        @Override
        public void run() {
            System.out.println(getName() + " started");
            while (!wasError) {
                try {
                    load();
                    //GAUSSIAN distribution
//                    int randomNum = random.nextInt((1000 - 500) + 1) + 500;
//                    Thread.sleep(TimeUnit.MILLISECONDS.toMillis(randomNum));
                    //BURST MODE
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException ignored) {
                    wasError = true;
                }
            }
        }

        void load() {
            try {
                readLock.lock();
                if (token == tokenSrc.get()) {
                    System.out.println(getName() + ": Success load with current token");
                } else {
                    System.out.println(getName() + ": Fail load with current token");
                    refreshToken(token);
                    if (token == tokenSrc.get()) {
                        System.out.println(getName() + ": Success load after token update");
                    } else {
                        System.out.println(getName() + ": Fail load totally");
                        wasError = true;
                    }
                }
            } finally {
                readLock.unlock();
            }

        }

        void refreshToken(int oldToken) {
            if (token == oldToken) {
                try {
                    readLock.unlock();
                    writeLock.lock();
                    //upgrade to write lock is not safe - so double check
                    if (token == oldToken) {
                        System.out.println(getName() + ": requesting new token");
                        token = requestToken();
                    } else {
                        System.out.println(getName() + ": No need to request token anymore");
                    }
                } finally {
                    // can safely downgrade
                    readLock.lock();
                    writeLock.unlock();
                }
            }
        }
    }

    static int requestToken() {
        try {
            requestCounter++;
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));//2 cycles of load should be max possible
            return tokenSrc.get() * requestCounter;
        } catch (InterruptedException ignored) {
            wasError = true;
            return -1;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>(11);
        final Thread tokenUpdater = new Thread(() -> {
            System.out.println("Token updater started");
            while (!wasError) {
                try {
                    if (requestCounter > 1) {
                        wasError = true;
                        System.out.println("More than one thread requested new token");
                    }
                    requestCounter = 0;
                    int expect = tokenSrc.get();
                    wasError = !tokenSrc.compareAndSet(expect, new Random().nextInt());
                    System.out.println("Changed tokenSrc value, success=" + !wasError);
                    Thread.sleep(TimeUnit.SECONDS.toMillis(9));//2 cycles of load should be max possible
                } catch (InterruptedException ignored) {
                    wasError = true;
                }
            }
        }, "tokenUpdater");
        threads.add(tokenUpdater);
        tokenUpdater.start();
        for (int i = 0; i < 16; i++) {
            Thread consumer = new ConsumerThread(i);
            threads.add(consumer);
            consumer.start();
        }
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                wasError = true;
                System.out.println("Failed to join to " + t.getName());
            }
        });
    }
}
