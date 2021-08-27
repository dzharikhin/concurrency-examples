import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Balancer {

  private static final AtomicBoolean changeBarrier = new AtomicBoolean();

  public static void main(String[] args) throws InterruptedException {
    Clock clock = Clock.systemDefaultZone();
    Instant start = clock.instant();
    ConcurrentHashMap<Integer, List<Integer>> counter = new ConcurrentHashMap<>();
    List<Server> servers = List.of(new Server(200), new Server(50));
    ExecutorService executorService = Executors.newFixedThreadPool(16);
    executorService.invokeAll(IntStream.range(0, 10_000).mapToObj(ignored -> (Callable<?>) () -> {
      int minIndex = -1;
      Weight minWeight = null;
      //here we need to freeze all servers stats - so we use write lock for read
      lockBarrier();
      try {
        for (int i = 0; i < servers.size(); i++) {
          Server server = servers.get(i);
          Weight weight = new Weight(server.getLoad());
          if ((minIndex < 0 || minWeight.compareTo(weight) > 0)) {
            minIndex = i;
            minWeight = weight;
          }
        }
        servers.get(minIndex).acquire();
      } finally {
        unlockBarrier();
      }
      // actual work with server
      Thread.sleep(10);
      counter.computeIfAbsent(minIndex, ign -> new CopyOnWriteArrayList<>()).add(1);
      release(minIndex, servers);
      return null;
    }).collect(Collectors.toList()));
    System.out.println("Execution time: " + Duration.between(start, clock.instant()).toMillis());
    System.out.println("final stat load - 0:" + servers.get(0).getStatLoad() + ",1:" + servers.get(1).getStatLoad());
    System.out.println("final request distribution - 0:" + counter.get(0).size() + ",1:" + counter.get(1).size() + ", ratio=" + (float) counter.get(0).size() / counter.get(1).size());
    executorService.shutdown();
  }

  private static void release(int index, List<Server> servers) {
    servers.get(index).release();
    boolean needToRescale;
    lockBarrier();
    try {
      needToRescale = servers.stream().allMatch(server -> server.getStatLoad() > 1);
    } finally {
      unlockBarrier();
    }
    if (needToRescale) {
      servers.forEach(Server::rescale);
    }
  }


  private static class Server {
    private volatile int weight;
    private AtomicInteger stats = new AtomicInteger();

    public Server(int weight) {
      this.weight = weight;
    }

    public float getLoad() {
      int statsValue = stats.intValue();
      int statRequests = extractStatRequests(statsValue);
      int currentRequests = extractCurrentRequests(statsValue);
      return (float) (statRequests + currentRequests) / weight;
    }

    float getStatLoad() {
      return (float) extractStatRequests(stats.intValue()) / weight;
    }

    void acquire() {
      stats.addAndGet(packValueToStatRequests(1) + 1);
    }

    void rescale() {
      stats.updateAndGet(currentValue -> {
        int statRequests = extractStatRequests(currentValue);
        if (statRequests < weight) {
          return currentValue;
        }
        int currentRequests = extractCurrentRequests(currentValue);
        checkBarrier();
        if (statRequests >= 32_767) {
          return packValueToStatRequests(statRequests % weight) + currentRequests;
        }
        return packValueToStatRequests(statRequests - weight) + currentRequests;
      });
    }

    void release() {
      stats.updateAndGet(i -> {
        checkBarrier();
        return i > 0 ? i - 1 : i;
      });
    }

    private int extractStatRequests(int value) {
      return value >> 16;
    }

    private int extractCurrentRequests(int value) {
      return value & 0xFFFF;
    }

    private int packValueToStatRequests(int value) {
      return value << 16;
    }
  }

  private static class Weight implements Comparable<Weight> {
    private static final Comparator<Weight> CMP = Comparator.comparingDouble(Weight::getLoad);
    private final float load;

    public Weight(float load) {
      this.load = load;
    }

    public float getLoad() {
      return load;
    }

    @Override
    public int compareTo(Weight o) {
      return CMP.compare(this, o);
    }
  }

  private static void lockBarrier() {
    while (!changeBarrier.compareAndSet(false, true)) {
      Thread.onSpinWait();
    }
  }

  private static void unlockBarrier() {
    while (!changeBarrier.compareAndSet(true, false)) {
      Thread.onSpinWait();
    }
  }

  private static void checkBarrier() {
    while (changeBarrier.get()) {
      Thread.onSpinWait();
    }
  }
}
