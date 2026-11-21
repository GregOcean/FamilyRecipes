package com.familyrecipes.service;

import com.familyrecipes.monitoring.RequestMetrics;
import com.familyrecipes.common.ApiException;
import com.familyrecipes.ai.AiRoutes;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.management.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Current measurements only; no privileged shell, SQL text, secrets or persistent telemetry. */
@Service
public class AdminMonitoringService {
    private final JdbcTemplate db;
    private final DataSource source;
    private final RequestMetrics traffic;
    private final TaskControl controls;
    private final AiRoutes routes;
    @Value("${app.translation.enabled:false}") private boolean translationEnabled;
    @Value("${app.classification.enabled:false}") private boolean classificationEnabled;
    @Value("${file.upload-dir:./uploads}") private String uploads;
    private long previousAt, previousRx, previousTx;
    public AdminMonitoringService(JdbcTemplate db,DataSource source,RequestMetrics traffic,TaskControl controls,AiRoutes routes) {
        this.db=db;this.source=source;this.traffic=traffic;this.controls=controls;this.routes=routes;
    }
    public Map<String,Object> snapshot() {
        var result=new LinkedHashMap<String,Object>();
        result.put("observedAt",Instant.now().toString()); result.put("hardware",hardware());
        result.put("application",application()); result.put("traffic",traffic.snapshot());
        result.put("database",database()); result.put("tasks",tasks()); return result;
    }
    private Map<String,Object> hardware() {
        var out=new LinkedHashMap<String,Object>(); var os=ManagementFactory.getOperatingSystemMXBean();
        out.put("os",os.getName());out.put("architecture",os.getArch());out.put("cpuCores",os.getAvailableProcessors());
        out.put("loadAverage",os.getSystemLoadAverage()<0?null:os.getSystemLoadAverage());
        if(os instanceof com.sun.management.OperatingSystemMXBean bean) {
            out.put("cpuPercent",percent(bean.getCpuLoad()));out.put("processCpuPercent",percent(bean.getProcessCpuLoad()));
            out.put("memoryTotalBytes",bean.getTotalMemorySize());out.put("memoryFreeBytes",bean.getFreeMemorySize());
            out.put("swapTotalBytes",bean.getTotalSwapSpaceSize());out.put("swapFreeBytes",bean.getFreeSwapSpaceSize());
        }
        try {
            // Linux MemAvailable includes reclaimable page cache; MemFree alone overstates memory pressure.
            for(String line:Files.readAllLines(Path.of("/proc/meminfo"))) if(line.startsWith("MemAvailable:"))
                out.put("memoryAvailableBytes",Long.parseLong(line.trim().split("\\s+")[1])*1024);
        }catch(Exception ignored){}
        try {
            Path p=Path.of(uploads).toAbsolutePath();while(!Files.exists(p)&&p.getParent()!=null)p=p.getParent();
            var disk=Files.getFileStore(p);out.put("diskTotalBytes",disk.getTotalSpace());out.put("diskFreeBytes",disk.getUsableSpace());
        }catch(Exception e){out.put("diskError","磁盘指标不可用");}
        out.put("network",network());return out;
    }
    private Object percent(double value){return value<0||!Double.isFinite(value)?null:value*100;}
    private synchronized Map<String,Object> network() {
        try {
            long rx=0,tx=0;for(String line:Files.readAllLines(Path.of("/proc/net/dev"))) {
                if(!line.contains(":"))continue;String[] pair=line.split(":",2);if(pair[0].trim().equals("lo"))continue;
                String[] fields=pair[1].trim().split("\\s+");rx+=Long.parseLong(fields[0]);tx+=Long.parseLong(fields[8]);
            }
            long now=System.nanoTime();var out=new LinkedHashMap<String,Object>();out.put("available",true);
            double elapsed=(now-previousAt)/1e9;
            out.put("rxBytesPerSecond",previousAt==0?null:Math.max(0,rx-previousRx)/elapsed);
            out.put("txBytesPerSecond",previousAt==0?null:Math.max(0,tx-previousTx)/elapsed);
            previousAt=now;previousRx=rx;previousTx=tx;return out;
        }catch(Exception e){return Map.of("available",false);}
    }
    private Map<String,Object> application() {
        var runtime=Runtime.getRuntime();var threads=ManagementFactory.getThreadMXBean();
        var out=new LinkedHashMap<String,Object>();out.put("status","up");out.put("javaVersion",System.getProperty("java.version"));
        out.put("uptimeSeconds",ManagementFactory.getRuntimeMXBean().getUptime()/1000);
        out.put("heapUsedBytes",runtime.totalMemory()-runtime.freeMemory());out.put("heapMaxBytes",runtime.maxMemory());
        out.put("threads",threads.getThreadCount());out.put("daemonThreads",threads.getDaemonThreadCount());
        long[] deadlocked=threads.findDeadlockedThreads();out.put("deadlockedThreads",deadlocked==null?0:deadlocked.length);
        out.put("gcCount",ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum());
        out.put("gcMillis",ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum());return out;
    }
    private Map<String,Object> database() {
        var out=new LinkedHashMap<String,Object>();long start=System.nanoTime();
        try {
            db.queryForObject("SELECT 1",Integer.class);out.put("status","up");out.put("latencyMillis",(System.nanoTime()-start)/1e6);
            if(source instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean()!=null) {
                var pool=hikari.getHikariPoolMXBean();out.put("pool",Map.of("active",pool.getActiveConnections(),"idle",pool.getIdleConnections(),
                    "total",pool.getTotalConnections(),"waiting",pool.getThreadsAwaitingConnection(),"max",hikari.getMaximumPoolSize()));
            }
            try {
                var status=new LinkedHashMap<String,Object>();
                for(var row:db.queryForList("SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Slow_queries','Queries','Uptime','Aborted_connects','Innodb_row_lock_current_waits','Innodb_buffer_pool_reads','Innodb_buffer_pool_read_requests')"))
                    status.put(row.get("Variable_name").toString(),Long.parseLong(row.get("Value").toString()));
                out.put("server",status);
            }catch(Exception e){out.put("serverUnavailable",true);}
            try { out.put("tables",db.queryForList("SELECT table_name AS name,table_rows AS estimatedRows,COALESCE(data_length,0)+COALESCE(index_length,0) AS bytes FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY COALESCE(data_length,0)+COALESCE(index_length,0) DESC LIMIT 12")); }
            catch(Exception e){out.put("tablesUnavailable",true);}
        }catch(Exception e){out.put("status","down");out.put("message","数据库暂不可用");}
        return out;
    }
    public Map<String,Object> tasks() {
        var out=new LinkedHashMap<String,Object>();
        out.put("translation",task("translation",translationEnabled,"content_translations"));
        out.put("classification",task("classification",classificationEnabled,"recipe_enrichment"));
        try {out.put("catalog",db.queryForList("SELECT status,COUNT(*) AS count FROM catalog_admin_job GROUP BY status"));}
        catch(Exception e){out.put("catalogUnavailable",true);}
        return out;
    }
    private Map<String,Object> task(String kind,boolean enabled,String table) {
        var out=new LinkedHashMap<String,Object>();out.put("enabled",enabled);out.put("configured",routes.route(AiRoutes.TRANSLATION).configured());
        out.put("paused",controls.isPaused(kind));out.put("model",routes.route(AiRoutes.TRANSLATION).getModel());
        try {out.put("counts",db.queryForList("SELECT status,COUNT(*) AS count FROM "+table+" GROUP BY status"));}
        catch(Exception e){out.put("unavailable",true);}
        return out;
    }
    public void control(String kind,String action) {
        if(!Set.of("translation","classification").contains(kind))throw new ApiException(400,"不支持的任务类型");
        if(!Set.of("pause","resume","retry").contains(action))throw new ApiException(400,"不支持的操作");
        if(!action.equals("pause") && (!(kind.equals("translation")?translationEnabled:classificationEnabled)||!routes.route(AiRoutes.TRANSLATION).configured()))
            throw new ApiException(409,"请先在服务器启用任务并配置模型密钥");
        if(action.equals("retry")) {
            String table=kind.equals("translation")?"content_translations":"recipe_enrichment";
            db.update("UPDATE "+table+" SET attempts=0,next_attempt_at=NOW()"+(kind.equals("translation")?",status='pending'":"")+" WHERE status='failed'");
        } else controls.change(kind,action.equals("pause"));
    }
}
