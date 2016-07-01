import java.util.concurrent.TimeUnit;

/**
 * Created by dzharikhin on 01.07.2016.
 */
public class ThreadSupportDaemon {

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            final long l = System.currentTimeMillis();
            while (System.currentTimeMillis() < l + TimeUnit.SECONDS.toMillis(10)) {
                System.out.println("I'm here");
            }
        });
        t1.setDaemon(true);
        t1.start();
    }
}
