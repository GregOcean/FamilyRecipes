package com.familyrecipes.monitoring;

import java.util.*;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

/** Bounded, in-memory 60-second window. Routes never contain user IDs, query text or credentials. */
@Component
public class RequestMetrics {
    private final LongSupplier clock;
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private long total, failures, requestBytes, responseBytes;
    private int active;
    private record Sample(long at, String route, int status, double millis, long in, long out) {}
    public RequestMetrics() { this(System::currentTimeMillis); }
    RequestMetrics(LongSupplier clock) { this.clock = clock; }
    public synchronized void started() { active++; }
    public synchronized void finished(String route, int status, double millis, long in, long out) {
        active = Math.max(0, active - 1); total++; if (status >= 500) failures++;
        requestBytes += Math.max(0, in); responseBytes += Math.max(0, out);
        samples.addLast(new Sample(clock.getAsLong(), route, status, millis, Math.max(0, in), Math.max(0, out)));
        trim();
    }
    private void trim() {
        long cutoff = clock.getAsLong() - 60_000;
        while (!samples.isEmpty() && (samples.peekFirst().at < cutoff || samples.size() > 20_000)) samples.removeFirst();
    }
    public synchronized Map<String,Object> snapshot() {
        trim(); var result = new LinkedHashMap<String,Object>();
        result.put("windowSeconds", 60); result.put("active", active); result.put("totalSinceStart", total);
        result.put("serverErrorsSinceStart", failures); result.put("requests", samples.size());
        result.put("requestsPerSecond", samples.size()/60.0);
        result.put("clientErrors", samples.stream().filter(s->s.status>=400 && s.status<500).count());
        result.put("serverErrors", samples.stream().filter(s->s.status>=500).count());
        result.put("inBytesPerSecond", samples.stream().mapToLong(Sample::in).sum()/60.0);
        result.put("outBytesPerSecond", samples.stream().mapToLong(Sample::out).sum()/60.0);
        result.put("requestBytesSinceStart", requestBytes); result.put("responseBytesSinceStart", responseBytes);
        var times = samples.stream().mapToDouble(Sample::millis).sorted().toArray();
        result.put("meanMillis", times.length==0?0:Arrays.stream(times).average().orElse(0));
        result.put("p95Millis", times.length==0?0:times[Math.max(0,(int)Math.ceil(times.length*.95)-1)]);
        var routes = new HashMap<String,List<Sample>>();
        for (var s:samples) routes.computeIfAbsent(s.route, k->new ArrayList<>()).add(s);
        result.put("routes", routes.entrySet().stream().sorted((a,b)->Integer.compare(b.getValue().size(),a.getValue().size())).limit(12)
            .map(e->Map.of("route",e.getKey(),"requests",e.getValue().size(),"errors",e.getValue().stream().filter(s->s.status>=500).count(),
                "meanMillis",e.getValue().stream().mapToDouble(Sample::millis).average().orElse(0))).toList());
        return result;
    }
}
