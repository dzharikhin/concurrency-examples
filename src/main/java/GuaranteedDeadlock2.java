import java.util.concurrent.CountDownLatch;

/**
 * Created by dzharikhin on 28.04.2016.
 */
public class GuaranteedDeadlock2 {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Thread t1 = new Thread(() -> {
            try {
                System.out.println("t1");
                latch.await();
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                System.out.println("t2");
                latch.await();
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
