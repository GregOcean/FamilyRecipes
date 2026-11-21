package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.ai.AiRoutes;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.*;

/** Single-host, bounded discovery jobs. Search candidates require explicit administrator review. */
@Service
public class CatalogAdminService {
    @Autowired private JdbcTemplate db;
    @Autowired private ObjectMapper json;
    @Autowired private AiRoutes ai;
    @Autowired private MediaService media;
    @Autowired private TransactionTemplate tx;
    @Value("${app.catalog.admin-user-id:0}") private long adminId;
    @Value("${app.catalog.jobs-dir:./catalog-jobs}") private String directory;
    @Value("${app.catalog.python:python3}") private String python;
    @Value("${CATALOG_SEARCH_KEY:}") private String searchKey;
    private Process process;
    private String runningId;
    private long started;
    private boolean recovered;
    public void requireAdmin(Long userId) {
        if(userId==null || adminId<=0 || userId!=adminId) throw new ApiException(403,"仅管理员可访问");
    }
    private Path root() throws IOException { Path p=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(p);return p; }
    private Path folder(String id) throws IOException { UUID.fromString(id);return root().resolve(id); }
    public Map<String,Object> recipes(String q, int page, boolean missing) {
        if(q.length()>100 || page<0 || page>10000)throw new ApiException(400,"查询参数不正确");
        String where=" FROM recipe r JOIN recipe_provenance p ON p.recipe_id=r.id WHERE r.visibility='public' AND (r.name LIKE ? OR EXISTS (SELECT 1 FROM content_translations t WHERE t.source_hash=SHA2(r.name,256) AND t.status='ready' AND t.translated_text LIKE ?))";
        if(missing)where+=" AND (COALESCE(r.cover_image,'')='' OR (SELECT COUNT(*) FROM external_recipe e WHERE e.recipe_id=r.id)<2)";
        String pattern="%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        var rows=db.queryForList("SELECT r.id,r.name,r.cover_image,(SELECT COUNT(*) FROM external_recipe e WHERE e.recipe_id=r.id) AS link_count"+where+" ORDER BY r.id LIMIT 30 OFFSET ?",pattern,pattern,page*30);
        return Map.of("items",rows,"total",db.queryForObject("SELECT COUNT(*)"+where,Long.class,pattern,pattern),"page",page);
    }
    public synchronized String create(List<Long> ids, Long userId) throws IOException {
        requireAdmin(userId);
        if(ids==null || ids.isEmpty() || ids.size()>30 || new HashSet<>(ids).size()!=ids.size())throw new ApiException(400,"每批请选择1至30道菜谱，不可重复");
        if(db.queryForObject("SELECT COUNT(*) FROM catalog_admin_job WHERE status IN ('queued','running')",Integer.class)>=5)throw new ApiException(409,"队列已有5批，请等待或暂停任务");
        if(key().isBlank())throw new ApiException(400,"服务器尚未配置搜索API密钥");
        List<Map<String,Object>> snapshots=new ArrayList<>();
        for(Long id:ids) {
            var rows=db.queryForList("SELECT r.id,r.name,IF(COALESCE(r.cover_image,'')<>'' OR EXISTS(SELECT 1 FROM recipe_image i WHERE i.recipe_id=r.id),1,0) AS has_image FROM recipe r JOIN recipe_provenance p ON p.recipe_id=r.id WHERE r.id=? AND r.visibility='public'",id);
            if(rows.isEmpty())throw new ApiException(400,"只能选择公开的初始化菜谱");
            var row=rows.get(0);
            row.put("existing_links",db.queryForList("SELECT url FROM external_recipe WHERE recipe_id=?",String.class,id));
            row.put("tags",db.queryForList("SELECT tag_value FROM recipe_tag WHERE recipe_id=?",String.class,id));
            for(var t:db.queryForList("SELECT target_language,translated_text FROM content_translations WHERE source_hash=SHA2(?,256) AND kind='content' AND status='ready' AND target_language IN ('zh','en') ORDER BY updated_at,id",row.get("name")))row.put("name_"+t.get("target_language"),t.get("translated_text"));
            snapshots.add(row);
        }
        String id=UUID.randomUUID().toString();Path p=folder(id);Files.createDirectories(p);
        String data=json.writeValueAsString(snapshots);Files.writeString(p.resolve("recipes.json"),data);
        db.update("INSERT INTO catalog_admin_job(id,status,recipes,created_by) VALUES(?,'queued',?,?)",id,data,userId);return id;
    }
    private String key() {return searchKey.isBlank()?ai.route(AiRoutes.TRANSLATION).getKey():searchKey;}
    public List<Map<String,Object>> jobs() {
        var rows=db.queryForList("SELECT id,status,recipes,JSON_LENGTH(recipes) AS total,created_at,updated_at,error FROM catalog_admin_job ORDER BY created_at DESC LIMIT 100");
        for(var row:rows) {
            Object recipes=row.remove("recipes");
            try {
                Path file=folder(row.get("id").toString()).resolve("results.json");
                JsonNode report=Files.exists(file)?json.readTree(file.toFile()):json.createObjectNode();
                row.put("progress",summarize(json.readTree(recipes.toString()),report,row.get("status").toString()));
            }catch(Exception e){row.put("progressError","进度文件暂不可读，请稍后刷新；任务状态以列表为准");}
        }
        return rows;
    }
    public static Map<String,Object> summarize(JsonNode recipes,JsonNode report,String status) {
        Map<Long,JsonNode> states=new HashMap<>();
        for(JsonNode j:report.path("jobs"))states.put(j.path("id").asLong(),j);
        List<Map<String,Object>> items=new ArrayList<>();int completed=0,failed=0,running=0;
        for(JsonNode recipe:recipes) {
            long id=recipe.path("id").asLong();JsonNode j=states.get(id);
            String state=j==null?"pending":j.path("status").asText("pending");
            if(state.equals("running")&&!status.equals("running"))state=status.equals("paused")?"paused":"interrupted";
            if(state.equals("done"))completed++;if(state.equals("failed"))failed++;if(state.equals("running"))running++;
            String stage="";
            if(state.equals("running"))stage=report.path("activity").path("recipe_id").asLong()==id?report.path("activity").path("stage").asText():"准备搜索";
            JsonNode result=j==null?null:j.path("result");
            items.add(Map.of("id",id,"name",recipe.path("name").asText(),"status",state,"stage",stage,
                "error",result==null?"":result.path("error").asText(""),"links",result==null?0:result.path("link_candidates").size(),"images",result==null?0:result.path("images").size()));
        }
        return Map.of("completed",completed,"failed",failed,"running",running,"total",recipes.size(),"items",items,
            "searchRequests",report.path("requests_by_kind").path("search").asInt(),"updatedAt",report.path("updated_at").asText(""));
    }
    private Map<String,Object> job(String id) {
        var rows=db.queryForList("SELECT id,status,recipes,error FROM catalog_admin_job WHERE id=?",id);
        if(rows.isEmpty())throw new ApiException(404,"任务不存在");return rows.get(0);
    }
    public Map<String,Object> result(String id) throws IOException {
        var row=job(id);Path file=folder(id).resolve("results.json");
        row.put("results",Files.exists(file)?json.readTree(file.toFile()):json.createObjectNode());return row;
    }
    public synchronized void action(String id,String action) {
        var row=job(id);String status=row.get("status").toString();
        if(action.equals("pause")) {
            if(id.equals(runningId) && process!=null)stopProcess();
            db.update("UPDATE catalog_admin_job SET status='paused',error=NULL WHERE id=?",id);
        } else if(action.equals("retry")) {
            if(Set.of("running","queued").contains(status))throw new ApiException(409,"任务已在队列中");
            if(db.queryForObject("SELECT COUNT(*) FROM catalog_admin_job WHERE status IN ('queued','running')",Integer.class)>=5)throw new ApiException(409,"队列已满");
            db.update("UPDATE catalog_admin_job SET status='queued',error=NULL WHERE id=?",id);
        } else throw new ApiException(400,"不支持的操作");
    }
    private void stopProcess() {
        if(process!=null){process.destroyForcibly();try{process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        process=null;runningId=null;
    }
    @jakarta.annotation.PreDestroy public synchronized void shutdown(){stopProcess();}
    @Scheduled(fixedDelay=3000) public synchronized void tick() {
        try {
            if(!recovered){db.update("UPDATE catalog_admin_job SET status='paused',error='服务重启，点击继续可复用缓存' WHERE status='running'");recovered=true;}
            if(process!=null) {
                if(process.isAlive()) {
                    if(System.currentTimeMillis()-started>Duration.ofHours(2).toMillis()){String id=runningId;stopProcess();db.update("UPDATE catalog_admin_job SET status='failed',error='超过2小时，任务已停止' WHERE id=?",id);}
                    return;
                }
                int exit=process.exitValue();Path result=folder(runningId).resolve("results.json");
                boolean failures=exit!=0;
                if(Files.exists(result))for(JsonNode j:json.readTree(result.toFile()).path("jobs"))if(!j.path("status").asText().equals("done"))failures=true;
                db.update("UPDATE catalog_admin_job SET status=?,error=? WHERE id=?",failures?"failed":"review",failures?"部分任务未完成，请查看候选中的失败原因；可继续重试（每菜最多3次，每批搜索最多120次）":null,runningId);
                process=null;runningId=null;
            }
            var next=db.queryForList("SELECT id FROM catalog_admin_job WHERE status='queued' ORDER BY created_at LIMIT 1");
            if(next.isEmpty())return;
            String id=next.get(0).get("id").toString();Path p=folder(id);
            try(var input=getClass().getResourceAsStream("/catalog/worker.py")){Files.copy(Objects.requireNonNull(input),p.resolve("worker.py"),StandardCopyOption.REPLACE_EXISTING);}
            ProcessBuilder builder=new ProcessBuilder(python,p.resolve("worker.py").toString(),"--state-dir",p.toString(),"--recipes-file",p.resolve("recipes.json").toString());
            // Do not pass database, mail, signing or unrelated credentials to the search subprocess.
            String path=System.getenv("PATH");builder.environment().clear();if(path!=null)builder.environment().put("PATH",path);
            builder.environment().put("CATALOG_SEARCH_KEY",key());builder.environment().put("PYTHONIOENCODING","utf-8");
            builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(p.resolve("worker.log").toFile()));
            runningId=id;started=System.currentTimeMillis();
            db.update("UPDATE catalog_admin_job SET status='running',error=NULL WHERE id=?",id);
            process=builder.start();
        } catch(Exception e) {
            if(runningId!=null)db.update("UPDATE catalog_admin_job SET status='failed',error='无法运行搜索任务，请检查Python3及任务目录权限' WHERE id=?",runningId);
            stopProcess();
        }
    }
    public Map<String,Object> monitor() throws IOException {
        var runtime=Runtime.getRuntime();
        return Map.of("database",db.queryForObject("SELECT 1",Integer.class)==1?"ok":"error","uptimeSeconds",java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime()/1000,
            "heapUsedMB",(runtime.totalMemory()-runtime.freeMemory())/1048576,"heapMaxMB",runtime.maxMemory()/1048576,
            "diskFreeMB",Files.getFileStore(root()).getUsableSpace()/1048576,"searchConfigured",!key().isBlank(),
            "jobs",db.queryForList("SELECT status,COUNT(*) AS count FROM catalog_admin_job GROUP BY status"),
            "translations",db.queryForList("SELECT status,COUNT(*) AS count FROM content_translations GROUP BY status"),
            "recentFailures",db.queryForList("SELECT id,error,updated_at FROM catalog_admin_job WHERE status='failed' ORDER BY updated_at DESC LIMIT 10"));
    }
    private static final Set<String> LICENSES=Set.of("CC0","Public domain","CC BY 2.0","CC BY 3.0","CC BY 4.0","CC BY-SA 2.0","CC BY-SA 3.0","CC BY-SA 4.0");
    public static URI imageUri(String url) {
        try {URI u=URI.create(url);if(!"https".equals(u.getScheme()) || !"upload.wikimedia.org".equals(u.getHost()) || u.getRawUserInfo()!=null || (u.getPort()!=-1&&u.getPort()!=443) || !u.getPath().startsWith("/wikipedia/commons/"))throw new IllegalArgumentException();return u;}
        catch(Exception e){throw new ApiException(400,"图片必须来自Wikimedia Commons");}
    }
    public synchronized void apply(String jobId,long recipeId,List<Integer> links,Integer image,Long userId) throws Exception {
        requireAdmin(userId);
        JsonNode report=(JsonNode)result(jobId).get("results");JsonNode candidate=null;
        for(JsonNode j:report.path("jobs"))if(j.path("id").asLong()==recipeId && j.path("status").asText().equals("done"))candidate=j.path("result");
        if(candidate==null)throw new ApiException(400,"此菜谱尚无可用候选");
        if(links==null || links.size()>10 || new HashSet<>(links).size()!=links.size())throw new ApiException(400,"链接选择无效");
        List<JsonNode> selected=new ArrayList<>();for(int n:links){if(n<0 || n>=candidate.path("link_candidates").size())throw new ApiException(400,"链接候选无效");selected.add(candidate.path("link_candidates").get(n));}
        JsonNode picture=null;String uploaded=null;String hash=null;
        if(image!=null) {
            if(image<0 || image>=candidate.path("images").size())throw new ApiException(400,"图片候选无效");
            picture=candidate.path("images").get(image);if(!LICENSES.contains(picture.path("license").asText()))throw new ApiException(400,"图片许可不支持");
            URI uri=imageUri(picture.path("url").asText());
            hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(uri.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if(db.queryForObject("SELECT COUNT(*) FROM catalog_media_credit WHERE recipe_id=? AND source_hash=?",Integer.class,recipeId,hash)==0) {
                uploaded=media.upload(new ImageFile(downloadImage(uri)),userId,"cover");
            }
        }
        final String url=uploaded,creditHash=hash;final JsonNode credit=picture;
        try {
            tx.executeWithoutResult(status->{
                var recipe=db.queryForList("SELECT r.id FROM recipe r JOIN recipe_provenance p ON p.recipe_id=r.id WHERE r.id=? AND r.visibility='public' FOR UPDATE",recipeId);
                if(recipe.isEmpty())throw new ApiException(409,"菜谱已删除或不再公开");
                for(JsonNode l:selected) {
                    String target=l.path("url").asText();if(target.length()>500)throw new ApiException(400,"链接过长");
                    if(db.queryForObject("SELECT COUNT(*) FROM external_recipe WHERE recipe_id=? AND url=?",Integer.class,recipeId,target)==0)
                        db.update("INSERT INTO external_recipe(recipe_id,title,url,source,notes,sort_order,added_by) SELECT ?,?,?,?, ?,COALESCE(MAX(sort_order),-1)+1,? FROM external_recipe WHERE recipe_id=?",recipeId,l.path("title").asText().substring(0,Math.min(200,l.path("title").asText().length())),target,source(target),"相关做法；根据标题和摘要匹配，具体配方可能不同",userId,recipeId);
                }
                if(url!=null) {
                    db.update("INSERT INTO recipe_image(recipe_id,url,category,caption,position) VALUES(?,?,'cover','',0)",recipeId,url);
                    db.update("UPDATE recipe SET cover_image=? WHERE id=? AND COALESCE(cover_image,'')=''",url,recipeId);
                    db.update("INSERT INTO catalog_media_credit(recipe_id,source_hash,metadata,media_url) VALUES(?,?,?,?)",recipeId,creditHash,credit.toString(),url);
                    try {
                        var provenance=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(db.queryForObject("SELECT metadata FROM recipe_provenance WHERE recipe_id=?",String.class,recipeId));
                        var entry=credit.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)entry).put("source_url",credit.path("page").asText()).put("modifications","Re-encoded as PNG; no intentional visual changes");
                        provenance.withArray("images").add(entry);
                        db.update("UPDATE recipe_provenance SET metadata=? WHERE recipe_id=?",provenance.toString(),recipeId);
                    } catch(java.io.IOException e){throw new IllegalStateException("Invalid provenance",e);}

                }
            });
        } catch(RuntimeException e){if(url!=null)media.retireIfUnlinked(url);throw e;}
    }
    public byte[] downloadImage(URI uri) throws IOException {
        imageUri(uri.toString());
        var conn=(javax.net.ssl.HttpsURLConnection)uri.toURL().openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(10000);conn.setReadTimeout(20000);conn.setRequestProperty("User-Agent","FamilyRecipes/1.0 (image attribution preserved)");
        try{if(conn.getResponseCode()!=200)throw new ApiException(502,"图片下载失败");try(var in=conn.getInputStream()){return in.readNBytes(10*1024*1024+1);}}finally{conn.disconnect();}
    }
    private String source(String url){String h=URI.create(url).getHost();if(h.endsWith("youtube.com")||h.equals("youtu.be"))return "YouTube";if(h.endsWith("bilibili.com"))return "哔哩哔哩";if(h.endsWith("xiaohongshu.com"))return "小红书";return "下厨房";}
    private record ImageFile(byte[] bytes) implements MultipartFile {
        public String getName(){return "image";}public String getOriginalFilename(){return "image.jpg";}public String getContentType(){return "application/octet-stream";}
        public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes;}
        public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(File file)throws IOException{Files.write(file.toPath(),bytes);}
    }
}
