/**
 * Created by dzharikhin on 28.04.2016.
 */
public class GuaranteedDeadlock1 {

    public static void main(String[] args) {
        final Object lock = new Object();
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("t1");
            }
        });
        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("t2");
                    t1.start();
                    t1.join();
                } catch (InterruptedException e) {
                }
            }
        });
        t2.start();
    }
}
