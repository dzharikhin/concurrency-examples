/**
 * Created by dzharikhin on 24.05.2016.
 */
public class Interruption {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            while (true) {
                System.out.println(Thread.currentThread().getName() + ": new computation cycle started");
                long initial = System.currentTimeMillis();
                try {
                    Thread.sleep(5000);
                    System.out.println(Thread.currentThread().getName() + ": I was finished, after " + getDuration(initial) + "ms");
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + ": I was interrupted, after " + getDuration(initial) + "ms");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    t1.interrupt();
                } catch (InterruptedException ignored) {
                    ignored.printStackTrace();
                }
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    private static long getDuration(long initial) {return System.currentTimeMillis() - initial;}
}
