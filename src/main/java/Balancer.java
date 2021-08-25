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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Balancer {

  public static void main(String[] args) throws InterruptedException {
    Clock clock = Clock.systemDefaultZone();
    Instant start = clock.instant();
    ConcurrentHashMap<Integer, List<Integer>> counter = new ConcurrentHashMap<>();
    List<Server> servers = List.of(new Server(200), new Server(50));
    ExecutorService executorService = Executors.newFixedThreadPool(16);
    executorService.invokeAll(IntStream.range(0, 10_000).mapToObj(ignored -> (Callable<?>) () -> {
      int minIndex = -1;
      Weight minWeight = null;
      for (int i = 0; i < servers.size(); i++) {
        Server server = servers.get(i);
        Weight weight = new Weight(server.getCurrentLoad(), server.getStatLoad());
        if ((minIndex < 0 || minWeight.compareTo(weight) > 0)) {
          minIndex = i;
          minWeight = weight;
        }
      }
      servers.get(minIndex).acquire();
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
    if (servers.stream().allMatch(server -> server.stat >= server.weight)) {
      servers.forEach(Server::rescale);
    }
  }


  private static class Server {
    private volatile int weight;
    private volatile int current;
    private volatile int stat;

    public Server(int weight) {
      this.weight = weight;
    }

    public float getCurrentLoad() {
      return (float) current / weight;
    }

    public float getStatLoad() {
      return (float) stat / weight;
    }

    synchronized void acquire() {
      current++;
      stat++;
    }

    synchronized void rescale() {
      stat -= weight;
    }

    synchronized void release() {
      if (current > 0) {
        current--;
      }
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
