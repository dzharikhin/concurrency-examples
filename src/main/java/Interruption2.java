/**
 * Created by dzharikhin on 24.05.2016.
 */
public class Interruption2 {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            while (true) {
                System.out.println(Thread.currentThread().getName() + ": new computation cycle started");
                long initial = System.currentTimeMillis();
                while(getDuration(initial) < 5000 && (!Thread.currentThread().isInterrupted() || getDuration(initial) < 500)) {}
                System.out.println(
                    Thread.currentThread().getName()
                    + ": I was finished, after "
                    + getDuration(initial)
                    + "ms, interrupted="
                    + Thread.currentThread().isInterrupted()
                );
            }
        });
        Thread t2 = new Thread(() -> {
            while (true) {
                System.out.println(Thread.currentThread().getName() + ": new computation cycle started");
                long initial = System.currentTimeMillis();
                while(getDuration(initial) < 5000 && !Thread.interrupted()) {}
                System.out.println(
                    Thread.currentThread().getName()
                        + ": I was finished, after "
                        + getDuration(initial)
                        + "ms, interrupted="
                        + Thread.currentThread().isInterrupted()
                );
            }
        });

        Thread t3 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    t1.interrupt();
                    t2.interrupt();
                } catch (InterruptedException ignored) {
                    ignored.printStackTrace();
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
    }

    private static long getDuration(long initial) {return System.currentTimeMillis() - initial;}
}
