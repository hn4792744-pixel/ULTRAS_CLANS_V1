package me.uc.hussein.ultrasclans.util;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * أداة مركزية لضمان قاعدة "Database async / Bukkit sync" (قسم 54).
 * كل عمليات قاعدة البيانات تُنفَّذ على executor منفصل، وأي تعديل بعدها
 * على Bukkit API (رسائل، GUI، إلخ) يعود دائمًا إلى الـmain thread.
 */
public final class Scheduler {

    private final UltrasClansPlugin plugin;
    private final Executor dbExecutor;

    public Scheduler(UltrasClansPlugin plugin, Executor dbExecutor) {
        this.plugin = plugin;
        this.dbExecutor = dbExecutor;
    }

    /** ينفذ مهمة (عادة JDBC) على thread قاعدة البيانات، وتُرجع النتيجة كـ CompletableFuture. */
    public <T> CompletableFuture<T> supplyAsync(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable job = () -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        };
        try {
            dbExecutor.execute(job);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // الـexecutor أُغلق (أثناء إيقاف السيرفر): ننفذ العملية فورًا بدل فقدان الحفظ
            job.run();
        }
        return future;
    }

    /** ينفذ Runnable (بدون نتيجة) على thread قاعدة البيانات. */
    public CompletableFuture<Void> runAsync(ThrowingRunnable task) {
        return supplyAsync(() -> {
            task.run();
            return null;
        });
    }

    /** يضمن تنفيذ consumer على الـmain thread، بغض النظر عن نجاح أو فشل الـfuture. */
    public <T> void onMainThread(CompletableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        future.whenComplete((result, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (throwable != null) {
                if (onError != null) {
                    onError.accept(throwable);
                } else {
                    plugin.getLogger().severe("Unhandled async error: " + throwable.getMessage());
                }
            } else if (onSuccess != null) {
                onSuccess.accept(result);
            }
        }));
    }

    public void runSync(Runnable runnable) {
        if (plugin.getServer().isPrimaryThread()) {
            runnable.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
