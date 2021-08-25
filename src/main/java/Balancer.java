import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

  private static final AtomicBoolean statsFrozen = new AtomicBoolean();

  public static void main(String[] args) throws InterruptedException {
    Clock clock = Clock.systemDefaultZone();
    Instant start = clock.instant();
    ConcurrentHashMap<Integer, List<Integer>> counter = new ConcurrentHashMap<>();
    List<Server> servers = List.of(new Server(0, 200), new Server(1, 50));
    ExecutorService executorService = Executors.newFixedThreadPool(16);
    executorService.invokeAll(IntStream.range(0, 10_000).mapToObj(ignored -> (Callable<?>) () -> {
      int minIndex = -1;
      Weight minWeight = null;
      while (!statsFrozen.compareAndSet(false, true)) {
        Thread.onSpinWait();
      }
      for (int i = 0; i < servers.size(); i++) {
        Server server = servers.get(i);
        Map.Entry<Float, Float> load = server.getLoad();
        Weight weight = new Weight(load.getValue(), load.getKey());
        if ((minIndex < 0 || minWeight.compareTo(weight) > 0)) {
          minIndex = i;
          minWeight = weight;
        }
      }
      servers.get(minIndex).acquire(minIndex);
      while (!statsFrozen.compareAndSet(true, false)) {
        Thread.onSpinWait();
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
    while (!statsFrozen.compareAndSet(false, true)) {
      Thread.onSpinWait();
    }
    boolean needRescale = servers.stream().allMatch(server -> server.getStatLoad() > 1);
    while (!statsFrozen.compareAndSet(true, false)) {
      Thread.onSpinWait();
    }
    if (needRescale) {
      servers.forEach(Server::rescale);
    }
  }


  private static class Server {
    private final int index;
    private volatile int weight;
    private AtomicInteger stats = new AtomicInteger();

    public Server(int index, int weight) {
      this.index = index;
      this.weight = weight;
    }

    public Map.Entry<Float, Float> getLoad() {
      int statsValue = stats.intValue();
      int statRequests = extractStatRequests(statsValue);
      int currentRequests = statsValue & 0xFFFF;
      return Map.entry((float) statRequests / weight, (float) currentRequests / weight);
    }

    float getStatLoad() {
      return (float) extractStatRequests(stats.intValue()) / weight;
    }

    void acquire(int index) {
      if (this.index != index) {
        while (statsFrozen.get()) {
          Thread.onSpinWait();
        }
      }
      stats.addAndGet(packValueToStatRequests(1) + 1);
    }

    void rescale() {
      while (statsFrozen.get()) {
        Thread.onSpinWait();
      }
      stats.updateAndGet(currentValue -> extractStatRequests(currentValue) >= weight ? currentValue - packValueToStatRequests(weight) : currentValue);
    }

    void release() {
      while (statsFrozen.get()) {
        Thread.onSpinWait();
      }
      stats.updateAndGet(i -> i > 0 ? i - 1 : i);
    }

    private int extractStatRequests(int value) {
      return value >>> 16;
    }

    private int packValueToStatRequests(int value) {
      return value << 16;
    }
  }

  private static class Weight implements Comparable<Weight> {
    private static final Comparator<Weight> CMP = Comparator.comparingDouble(Weight::getCurrent).thenComparingDouble(Weight::getStat);
    private final float current;
    private final float stat;

    public Weight(float current, float stat) {
      this.current = current;
      this.stat = stat;
    }

    public float getCurrent() {
      return current;
    }

    public float getStat() {
      return stat;
    }

    @Override
    public int compareTo(Weight o) {
      return CMP.compare(this, o);
    }
  }
}
