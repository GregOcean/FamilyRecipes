package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import com.familyrecipes.mapper.MediaMapper;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;

/** Local immutable images. Database records own them; files are never served as static assets. */
@Service
public class MediaService {
    @Value("${file.upload-dir}") private String directory;
    @Value("${file.base-url}") private String baseUrl;
    @Autowired private MediaMapper mapper;
    public String upload(MultipartFile file, Long userId) throws IOException {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new ApiException(400, "图片大小须为1字节至10MB");
        Path root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String name = UUID.randomUUID() + ".png";
        Path target = root.resolve(name);
        try (var stream = file.getInputStream(); var input = ImageIO.createImageInputStream(stream)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ApiException(400, "请上传JPEG或PNG图片");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
                    throw new ApiException(400, "请上传JPEG或PNG图片");
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 20_000_000) throw new ApiException(400, "图片不能超过2000万像素");
                if (!ImageIO.write(reader.read(0), "png", target.toFile()))
                    throw new IOException("Cannot encode image");
                mapper.insert(name, userId);
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(target);
                throw e;
            } finally { reader.dispose(); }
        }
        return baseUrl + name;
    }
    public Path read(String filename, Long viewerId) {
        if (!filename.matches("[a-f0-9-]{36}\\.png")) throw new ApiException(404, "图片不存在");
        Long owner = mapper.owner(filename);
        if (owner == null || (!owner.equals(viewerId) && mapper.visibleReferences(baseUrl + filename, viewerId) + mapper.fridgeReferences(baseUrl + filename, viewerId) + mapper.stickerReferences(baseUrl + filename, viewerId) == 0))
            throw new ApiException(404, "图片不存在或不可访问");
        Path path = Path.of(directory).toAbsolutePath().normalize().resolve(filename);
        if (!Files.isRegularFile(path)) throw new ApiException(404, "图片不存在");
        return path;
    }
    public String copyForOwner(String url,Long userId) {
        if(url==null || !url.startsWith(baseUrl)) return url;
        String filename=url.substring(baseUrl.length());
        Path original=read(filename,userId);
        String name=UUID.randomUUID()+".png";
        try { Files.copy(original,original.getParent().resolve(name));mapper.insert(name,userId); }
        catch(IOException e) { throw new ApiException(500,"图片复制失败",e); }
        return baseUrl+name;
    }
    public void requireOwned(String url, Long userId) {
        if (url == null || !url.contains("/api/upload/files/")) return;
        if (!url.startsWith(baseUrl)) throw new ApiException(400, "本地图片地址不合法");
        String name = url.substring(baseUrl.length());
        if (!userId.equals(mapper.owner(name))) throw new ApiException(400, "不能引用其他用户上传的图片");
    }

    public void validateCategory(String category) {
        if(category==null || !java.util.Set.of("cover","step","screenshot","cooking_record","inventory","sticker","other").contains(category))
            throw new ApiException(400,"图片分类不合法");
    }
    public String upload(MultipartFile file, Long userId,String category) throws IOException {
        validateCategory(category);String url=upload(file,userId);
        mapper.category(url.substring(baseUrl.length()),category);return url;
    }
    public void requireSticker(String url,Long userId) {
        if(url==null || !url.startsWith(baseUrl)) throw new ApiException(400,"表情包须上传为图片");
        requireOwned(url,userId);
        if(!"sticker".equals(mapper.categoryOf(url.substring(baseUrl.length())))) throw new ApiException(400,"图片未分类为表情包");
    }
    public java.util.List<java.util.Map<String,Object>> trash(Long userId) { return mapper.trash(userId); }
    @org.springframework.transaction.annotation.Transactional
    public void changeTrash(String filename,Long userId,boolean restore) {
        var row=mapper.lock(filename);
        if(row==null || ((Number)row.get("owner_id")).longValue()!=userId) throw new ApiException(404,"图片不存在");
        if(restore) {
            if(row.get("purged_at")!=null) throw new ApiException(410,"图片已物理清理");
            mapper.restore(filename);
        } else {
            if(mapper.references(baseUrl+filename)>0) throw new ApiException(409,"图片仍被内容引用，请先移除引用");
            mapper.trashFile(filename);
        }
    }
    public void retireIfUnlinked(String url) {
        if(url==null || !url.startsWith(baseUrl))return;
        String name=url.substring(baseUrl.length());
        if(mapper.lock(name)!=null && mapper.references(url)==0)mapper.trashFile(name);
    }

    // Each candidate is locked with its reference checks and deletion in one transaction.
    @Autowired private org.springframework.transaction.support.TransactionTemplate transactions;
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=3600000)
    public void cleanTrash() {
        String after="";
        while(true) {
            var batch=mapper.candidates(after);
            if(batch.isEmpty()) break;
            after=batch.get(batch.size()-1);
            for(String name:batch) transactions.executeWithoutResult(status -> {
            mapper.lock(name);
            if(mapper.references(baseUrl+name)>0) return;
            mapper.trashFile(name);
            if(mapper.due(name)>0) {
                try { Files.deleteIfExists(Path.of(directory).toAbsolutePath().normalize().resolve(name));mapper.purged(name); }
                catch(IOException e) { throw new java.io.UncheckedIOException(e); }
            }
        });
        }
    }
}
