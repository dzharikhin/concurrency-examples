import java.util.concurrent.TimeUnit;

public class NestedThreadException {

  public static void main(String[] args) throws InterruptedException {
    Thread innerThread = new Thread(() -> {
      throw new RuntimeException();
    }, "inner");
    innerThread.start();
    while (true) {
      System.out.println(Thread.currentThread().getName());
      Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    }
  }
}
