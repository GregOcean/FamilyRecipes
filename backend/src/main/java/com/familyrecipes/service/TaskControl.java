package com.familyrecipes.service;

import com.familyrecipes.common.ApiException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Operator pauses affect the next job; active requests finish normally. No credentials are exposed. */
@Service
public class TaskControl {
    private static final Set<String> KINDS = Set.of("translation", "classification");
    private final Set<String> paused = ConcurrentHashMap.newKeySet();
    public boolean isPaused(String kind) { return paused.contains(kind); }
    public void change(String kind, boolean pause) {
        if (!KINDS.contains(kind)) throw new ApiException(400, "不支持的任务类型");
        if (pause) paused.add(kind); else paused.remove(kind);
    }
}
