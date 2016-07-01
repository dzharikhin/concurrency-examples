import java.util.concurrent.TimeUnit;

/**
 * Created by dzharikhin on 01.07.2016.
 */
public class ThreadSupportNonDaemon {

    private static void sleepSilently() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread("inner") {
            @Override
            public void run() {
                final long l = System.currentTimeMillis();
                while (System.currentTimeMillis() < l + TimeUnit.SECONDS.toMillis(10)) {
                    System.out.println(getName());
                    if (true) {
                        throw new RuntimeException();
                    }
                    sleepSilently();
                }
            }
        };
        t1.start();
        sleepSilently();
        System.out.println("main");
    }
}
