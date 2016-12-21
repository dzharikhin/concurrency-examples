import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ostzrd on 21.12.2016.
 */
public class StampedLockExample {

    static String sharedValue;
    static Random random = new Random();

    public static void main(String[] args) {
        final StampedLock lock = new StampedLock();
        final List<Actor> threads = IntStream.range(0, 16)
                .mapToObj(i -> new Actor(i, lock))
                .collect(Collectors.toList());
        threads.forEach(actor -> {
                actor.start();
                System.out.println("Started thread " + actor.getName());
        });
        threads.forEach(actor -> {
            try {
                actor.join();
            } catch (InterruptedException ignored) {
            }
        });
    }

    static class Actor extends Thread {

        private final StampedLock lock;

        public Actor(int index, StampedLock lock) {
            super("actor-" + index);
            this.lock = lock;
        }

        @Override
        public void run() {
            IntStream.iterate(0, i -> i + 1).forEach(i -> {
                try {
                    if (i % 3 == 0) {//Here is consumer/producer distribution
                        write();
                    } else {
                        read();
                    }
                    Thread.sleep(TimeUnit.SECONDS.toMillis(random.nextInt(6)));
                } catch (InterruptedException ignored) {
                }
            });
        }

        private String read() {
            String result;
            final long optimisticReadStamp = lock.tryOptimisticRead();
            result = sharedValue;
            System.out.println("Thread: " + getName() +  " - optimistic read value: " + result);
            if (!lock.validate(optimisticReadStamp)) {
                System.out.println("Thread: " + getName() +  " - failed optimistic read");
                final long readLockStamp = lock.readLock();
                try {
                    result = sharedValue;
                    System.out.println("Thread: " + getName() +  " - read lock value: " + result);
                } finally {
                    lock.unlock(readLockStamp);
                }
            }
            return result;
        }

        private void write() throws InterruptedException {
            final long writeStamp = lock.writeLock();
            try {
                sharedValue = getName();
                System.out.println("Thread: " + getName() +  " - write value: " + getName());
            } finally {
                lock.unlockWrite(writeStamp);
            }
        }

    }
}
