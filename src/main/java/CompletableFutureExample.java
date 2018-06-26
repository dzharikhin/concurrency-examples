import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class CompletableFutureExample {

  public static void main(String[] args) throws InterruptedException {
    CompletableFuture<Integer> future = new CompletableFuture<>();
    new Thread(() -> {
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        future.complete(1);
      } catch (InterruptedException ignored) {
      }
    }).start();
    try {
      System.out.println(future.thenApply(value -> {
        if (value == 1) {
          throw new OneException();
        } else if (value == 2) {
          throw new TwoException();
        } else {
          return value;
        }
      }).get());
    } catch (ExecutionException e) {
      throw (RuntimeException) e.getCause();
    }
  }

  private static class OneException extends RuntimeException {}

  private static class TwoException extends RuntimeException {}
}
