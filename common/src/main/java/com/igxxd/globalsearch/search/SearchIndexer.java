package com.igxxd.globalsearch.search;

import com.igxxd.globalsearch.Constants;
import com.igxxd.globalsearch.search.provider.*;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Main coordinator for indexing search entries across Minecraft and installed mods.
 * Utilizes a pluggable {@link SearchIndexProvider} architecture so providers can be
 * maintained independently without hardcoded dependencies in this core class.
 * All providers can be processed concurrently in background daemon threads.
 */
public class SearchIndexer {

    private static volatile List<SearchResult> CACHED_RESULTS = null;
    private static final AtomicBoolean IS_INDEXING = new AtomicBoolean(false);

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())),
            r -> {
                Thread t = new Thread(r, "GlobalSearch-AsyncIndexer");
                t.setDaemon(true);
                return t;
            }
    );

    private static final List<SearchIndexProvider> PROVIDERS = new ArrayList<>(List.of(
            new KeybindIndexProvider(),
            new VanillaSettingIndexProvider(),
            new ItemIndexProvider(),
            new EnchantmentIndexProvider(),
            new PotionIndexProvider(),
            new BiomeIndexProvider(),
            new DimensionIndexProvider(),
            new StructureIndexProvider(),
            new GameRuleIndexProvider(),
            new StatisticIndexProvider(),
            new AdvancementIndexProvider(),
            new ModListIndexProvider(),
            new MalilibConfigIndexProvider(),
            new ClothConfigIndexProvider(),
            new NeoForgeConfigIndexProvider(),
            new XaeroConfigIndexProvider()
    ));

    /**
     * Register a custom search index provider dynamically.
     */
    public static synchronized void registerProvider(SearchIndexProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static boolean isIndexing() {
        return IS_INDEXING.get();
    }

    public static boolean hasCachedResults() {
        return CACHED_RESULTS != null;
    }

    public static List<SearchResult> getCachedResults() {
        return CACHED_RESULTS != null ? CACHED_RESULTS : Collections.emptyList();
    }

    public static synchronized List<SearchResult> getOrIndex(Minecraft minecraft) {
        if (CACHED_RESULTS == null) {
            CACHED_RESULTS = indexAll(minecraft);
        }
        return CACHED_RESULTS;
    }

    public static synchronized int rescan(Minecraft minecraft) {
        CACHED_RESULTS = indexAll(minecraft);
        return CACHED_RESULTS.size();
    }

    /**
     * Process all providers asynchronously across thread pool.
     */
    public static CompletableFuture<List<SearchResult>> indexAllAsync(Minecraft minecraft, Consumer<List<SearchResult>> onComplete) {
        return runAsyncIndexing(minecraft, false, onComplete);
    }

    /**
     * Force a full background rescan across all providers.
     */
    public static CompletableFuture<List<SearchResult>> rescanAsync(Minecraft minecraft, Consumer<List<SearchResult>> onComplete) {
        return runAsyncIndexing(minecraft, true, onComplete);
    }

    private static CompletableFuture<List<SearchResult>> runAsyncIndexing(Minecraft minecraft, boolean force, Consumer<List<SearchResult>> onComplete) {
        if (minecraft == null) {
            List<SearchResult> empty = Collections.emptyList();
            if (onComplete != null) onComplete.accept(empty);
            return CompletableFuture.completedFuture(empty);
        }

        if (!force && CACHED_RESULTS != null) {
            if (onComplete != null) {
                onComplete.accept(CACHED_RESULTS);
            }
            return CompletableFuture.completedFuture(CACHED_RESULTS);
        }

        if (IS_INDEXING.compareAndSet(false, true)) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    List<CompletableFuture<List<SearchResult>>> futures = new ArrayList<>();
                    for (SearchIndexProvider provider : PROVIDERS) {
                        futures.add(CompletableFuture.supplyAsync(() -> {
                            List<SearchResult> list = new ArrayList<>();
                            Set<String> seen = new HashSet<>();
                            try {
                                provider.index(minecraft, list, seen);
                            } catch (Throwable t) {
                                Constants.LOG.debug("Provider '{}' failed or skipped: {}", provider.getName(), t.getMessage());
                            }
                            return list;
                        }, ASYNC_EXECUTOR));
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                    List<SearchResult> combined = new ArrayList<>();
                    Set<String> globalSeen = new HashSet<>();
                    for (CompletableFuture<List<SearchResult>> f : futures) {
                        try {
                            for (SearchResult res : f.get()) {
                                String key = res.getType().name() + ":" + res.getPath().toLowerCase(Locale.ROOT);
                                if (globalSeen.add(key)) {
                                    combined.add(res);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }

                    Constants.LOG.info("Async indexed {} total search entries across {} providers in background.", combined.size(), PROVIDERS.size());
                    CACHED_RESULTS = Collections.unmodifiableList(combined);
                    return CACHED_RESULTS;
                } finally {
                    IS_INDEXING.set(false);
                }
            }, ASYNC_EXECUTOR).thenApply(results -> {
                if (onComplete != null && minecraft != null) {
                    minecraft.execute(() -> onComplete.accept(results));
                }
                return results;
            });
        } else {
            // Already indexing; return future waiting for cache to finish
            return CompletableFuture.supplyAsync(() -> {
                while (IS_INDEXING.get()) {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                List<SearchResult> res = CACHED_RESULTS;
                return res != null ? res : Collections.<SearchResult>emptyList();
            }, ASYNC_EXECUTOR).thenApply(results -> {
                if (onComplete != null && minecraft != null) {
                    minecraft.execute(() -> onComplete.accept(results));
                }
                return results;
            });
        }
    }

    public static List<SearchResult> indexAll(Minecraft minecraft) {
        List<SearchResult> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        if (minecraft == null) {
            return results;
        }

        for (SearchIndexProvider provider : PROVIDERS) {
            try {
                provider.index(minecraft, results, seenKeys);
            } catch (Throwable t) {
                Constants.LOG.debug("Provider '{}' failed or was skipped: {}", provider.getName(), t.getMessage());
            }
        }

        Constants.LOG.info("Indexed {} total global search entries across {} providers.", results.size(), PROVIDERS.size());
        CACHED_RESULTS = Collections.unmodifiableList(results);
        return CACHED_RESULTS;
    }

    /**
     * Delegates an edit action to the result's custom handler, its bound provider,
     * or queries registered providers.
     */
    public static boolean handleEdit(SearchResult result, net.minecraft.client.gui.screens.Screen screen, java.util.function.Consumer<String> onValueChanged) {
        if (result == null) return false;
        Minecraft mc = Minecraft.getInstance();

        // 1. Direct handler on SearchResult
        if (result.getEditHandler() != null) {
            try {
                if (result.getEditHandler().handleEdit(mc, screen, onValueChanged)) {
                    return true;
                }
            } catch (Throwable t) {
                Constants.LOG.error("Error executing result edit handler: {}", t.getMessage());
            }
        }

        // 2. Bound provider on SearchResult
        if (result.getProvider() != null) {
            try {
                if (result.getProvider().handleEdit(mc, result, screen, onValueChanged)) {
                    return true;
                }
            } catch (Throwable t) {
                Constants.LOG.error("Error in provider '{}' edit handler: {}", result.getProvider().getName(), t.getMessage());
            }
        }

        // 3. Fallback across all registered providers
        for (SearchIndexProvider provider : PROVIDERS) {
            try {
                if (provider.handleEdit(mc, result, screen, onValueChanged)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        return false;
    }
}
