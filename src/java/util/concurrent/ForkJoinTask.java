/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.Constructor;

/**
 * Abstract base class for tasks that run within a {@link ForkJoinPool}.
 * A {@code ForkJoinTask} is a thread-like entity that is much
 * lighter weight than a normal thread.  Huge numbers of tasks and
 * subtasks may be hosted by a small number of actual threads in a
 * ForkJoinPool, at the price of some usage limitations.
 * <p>
 *     在 ForkJoinPool 内运行的任务的抽象基类。 ForkJoinTask 是一个类似线程的实体，比普通线程的重量轻得多。
 *     在 ForkJoinPool 中，大量任务和子任务可以由少量实际线程托管，但会受到一些使用限制。
 * </p>
 *
 * <p>A "main" {@code ForkJoinTask} begins execution when it is
 * explicitly submitted to a {@link ForkJoinPool}, or, if not already
 * engaged in a ForkJoin computation, commenced in the {@link
 * ForkJoinPool#commonPool()} via {@link #fork}, {@link #invoke}, or
 * related methods.  Once started, it will usually in turn start other
 * subtasks.  As indicated by the name of this class, many programs
 * using {@code ForkJoinTask} employ only methods {@link #fork} and
 * {@link #join}, or derivatives such as {@link
 * #invokeAll(ForkJoinTask...) invokeAll}.  However, this class also
 * provides a number of other methods that can come into play in
 * advanced usages, as well as extension mechanics that allow support
 * of new forms of fork/join processing.
 * 
 * <p>
 *     一个 "ForkJoinTask"在明确提交给 ForkJoinPool 时开始执行，如果尚未参与 ForkJoin 计算，则通过 fork() 、 invoke() 或相关方法在 ForkJoinPool.commonPool() 中开始执行。
 *     一旦启动，它通常会反过来启动其他子任务。正如该类的名称所示，许多使用 ForkJoinTask 的程序只使用 fork() 和 join() 方法，或 invokeAll 等衍生方法。 
 *     不过，该类还提供了许多其他方法，可以在高级用法中发挥作用，还提供了扩展机制，可以支持新形式的 fork/join 处理。
 * </p>
 *
 * <p>A {@code ForkJoinTask} is a lightweight form of {@link Future}.
 * The efficiency of {@code ForkJoinTask}s stems from a set of
 * restrictions (that are only partially statically enforceable)
 * reflecting their main use as computational tasks calculating pure
 * functions or operating on purely isolated objects.  The primary
 * coordination mechanisms are {@link #fork}, that arranges
 * asynchronous execution, and {@link #join}, that doesn't proceed
 * until the task's result has been computed.  Computations should
 * ideally avoid {@code synchronized} methods or blocks, and should
 * minimize other blocking synchronization apart from joining other
 * tasks or using synchronizers such as Phasers that are advertised to
 * cooperate with fork/join scheduling. Subdividable tasks should also
 * not perform blocking I/O, and should ideally access variables that
 * are completely independent of those accessed by other running
 * tasks. These guidelines are loosely enforced by not permitting
 * checked exceptions such as {@code IOExceptions} to be
 * thrown. However, computations may still encounter unchecked
 * exceptions, that are rethrown to callers attempting to join
 * them. These exceptions may additionally include {@link
 * RejectedExecutionException} stemming from internal resource
 * exhaustion, such as failure to allocate internal task
 * queues. Rethrown exceptions behave in the same way as regular
 * exceptions, but, when possible, contain stack traces (as displayed
 * for example using {@code ex.printStackTrace()}) of both the thread
 * that initiated the computation as well as the thread actually
 * encountering the exception; minimally only the latter.
 * <p>
 *     ForkJoinTask 是 Future 的轻量级形式。 ForkJoinTask 的效率源于一系列限制（仅部分可静态执行），反映了它们作为计算任务计算纯函数或操作纯孤立对象的主要用途。
 *     主要的协调机制是 fork() 和 join() ，前者安排异步执行，后者在计算出任务结果后才继续执行。
 *     在理想情况下，计算应避免使用 synchronized 方法或块，除了加入其他任务或使用同步器（如相位器）外，还应尽量减少其他阻塞同步，因为相位器可与 fork/join 调度合作。
 *     可细分任务也不应执行阻塞式 I/O，最好访问完全独立于其他运行任务访问的变量。通过不允许抛出 IOExceptions 等已检查异常，这些准则得到了松散的执行。 
 *     不过，计算仍可能遇到未检查的异常，这些异常会被重新抛给试图加入计算的调用者。
 *     这些异常可能还包括 RejectedExecutionException 源自内部资源耗尽的异常，例如无法分配内部任务队列。被
 *     重新抛出的异常与常规异常的行为方式相同，但在可能的情况下，会同时包含启动计算的线程和实际遇到异常的线程的堆栈跟踪（例如使用 ex.printStackTrace() 显示）；但最少只包含后者。
 * </p>
 * <p>It is possible to define and use ForkJoinTasks that may block,
 * but doing do requires three further considerations: (1) Completion
 * of few if any <em>other</em> tasks should be dependent on a task
 * that blocks on external synchronization or I/O. Event-style async
 * tasks that are never joined (for example, those subclassing {@link
 * CountedCompleter}) often fall into this category.  (2) To minimize
 * resource impact, tasks should be small; ideally performing only the
 * (possibly) blocking action. (3) Unless the {@link
 * ForkJoinPool.ManagedBlocker} API is used, or the number of possibly
 * blocked tasks is known to be less than the pool's {@link
 * ForkJoinPool#getParallelism} level, the pool cannot guarantee that
 * enough threads will be available to ensure progress or good
 * performance.
 * <p>
 *     定义和使用可能阻塞的 ForkJoinTasks 是可能的，但这样做需要进一步考虑以下三点：
 *     (1) 很少有其他任务的完成依赖于外部同步或 I/O 阻塞的任务。从未被连接的事件式异步任务（例如，那些子类化 CountedCompleter 的任务）通常属于这一类。
 *     (2) 为尽量减少对资源的影响，任务应该很小；最好只执行（可能的）阻塞操作。
 *     (3) 除非使用 ForkJoinPool.ManagedBlocker API，或已知可能阻塞的任务数量少于池的 ForkJoinPool.getParallelism() 级别，否则池无法保证有足够的线程可用以确保进度或良好性能。
 * </p>
 * <p>The primary method for awaiting completion and extracting
 * results of a task is {@link #join}, but there are several variants:
 * The {@link Future#get} methods support interruptible and/or timed
 * waits for completion and report results using {@code Future}
 * conventions. Method {@link #invoke} is semantically
 * equivalent to {@code fork(); join()} but always attempts to begin
 * execution in the current thread. The "<em>quiet</em>" forms of
 * these methods do not extract results or report exceptions. These
 * may be useful when a set of tasks are being executed, and you need
 * to delay processing of results or exceptions until all complete.
 * Method {@code invokeAll} (available in multiple versions)
 * performs the most common form of parallel invocation: forking a set
 * of tasks and joining them all.
 * <p>
 *     等待任务完成和提取任务结果的主要方法是 join() ，但也有几种变体： Future.get() 方法支持可中断和/或定时等待完成，并使用 Future 惯例报告结果。
 *     方法 invoke() 在语义上等同于 fork(); join() ，但总是尝试在当前线程中开始执行。
 *     这些方法的 "安静 "形式不会提取结果或报告异常。当正在执行一组任务，需要延迟处理结果或异常，直到所有任务都完成时，这些方法可能会很有用。
 *     方法 invokeAll （有多个版本）执行最常见的并行调用形式：分叉一组任务并将它们全部连接起来。
 * </p>
 * <p>In the most typical usages, a fork-join pair act like a call
 * (fork) and return (join) from a parallel recursive function. As is
 * the case with other forms of recursive calls, returns (joins)
 * should be performed innermost-first. For example, {@code a.fork();
 * b.fork(); b.join(); a.join();} is likely to be substantially more
 * efficient than joining {@code a} before {@code b}.
 * <p>
 *     在最典型的用法中，fork-join 对就像并行递归函数的调用（fork）和返回（join）。与其他形式的递归调用一样，返回（连接）应最内层优先执行。
 *     例如， a.fork(); b.fork(); b.join(); a.join(); 的效率可能远远高于先连接 a 再连接 b 的效率。
 * </p>
 * <p>The execution status of tasks may be queried at several levels
 * of detail: {@link #isDone} is true if a task completed in any way
 * (including the case where a task was cancelled without executing);
 * {@link #isCompletedNormally} is true if a task completed without
 * cancellation or encountering an exception; {@link #isCancelled} is
 * true if the task was cancelled (in which case {@link #getException}
 * returns a {@link java.util.concurrent.CancellationException}); and
 * {@link #isCompletedAbnormally} is true if a task was either
 * cancelled or encountered an exception, in which case {@link
 * #getException} will return either the encountered exception or
 * {@link java.util.concurrent.CancellationException}.
 * <p>
 *     任务的执行状态可在多个详细级别上查询：如果任务以任何方式完成（包括任务在未执行的情况下被取消），则 isDone() 为 true；
 *     如果任务在未取消或遇到异常的情况下完成，则 isCompletedNormally() 为 true；
 *     如果任务被取消，则 isCancelled() 为 true（在这种情况下 getException() 返回 CancellationException ）；
 *     如果任务被取消或遇到异常，则 isCompletedAbnormally() 为 true，在这种情况下 getException() 返回遇到的异常或 CancellationException 。
 * </p>
 * <p>The ForkJoinTask class is not usually directly subclassed.
 * Instead, you subclass one of the abstract classes that support a
 * particular style of fork/join processing, typically {@link
 * RecursiveAction} for most computations that do not return results,
 * {@link RecursiveTask} for those that do, and {@link
 * CountedCompleter} for those in which completed actions trigger
 * other actions.  Normally, a concrete ForkJoinTask subclass declares
 * fields comprising its parameters, established in a constructor, and
 * then defines a {@code compute} method that somehow uses the control
 * methods supplied by this base class.
 * <p>
 *     通常不会直接子类化 ForkJoinTask 类。相反，你需要子类化一个支持特定 fork/join 处理风格的抽象类，
 *     通常 RecursiveAction 适用于大多数不返回结果的计算， RecursiveTask 适用于返回结果的计算， CountedCompleter 适用于已完成的操作会触发其他操作的计算。
 *     通常，具体的 ForkJoinTask 子类会声明包含其参数的字段，并在构造函数中建立，然后定义一个 compute 方法，该方法会以某种方式使用该基类提供的控制方法。
 * </p>
 * <p>Method {@link #join} and its variants are appropriate for use
 * only when completion dependencies are acyclic; that is, the
 * parallel computation can be described as a directed acyclic graph
 * (DAG). Otherwise, executions may encounter a form of deadlock as
 * tasks cyclically wait for each other.  However, this framework
 * supports other methods and techniques (for example the use of
 * {@link Phaser}, {@link #helpQuiesce}, and {@link #complete}) that
 * may be of use in constructing custom subclasses for problems that
 * are not statically structured as DAGs. To support such usages, a
 * ForkJoinTask may be atomically <em>tagged</em> with a {@code short}
 * value using {@link #setForkJoinTaskTag} or {@link
 * #compareAndSetForkJoinTaskTag} and checked using {@link
 * #getForkJoinTaskTag}. The ForkJoinTask implementation does not use
 * these {@code protected} methods or tags for any purpose, but they
 * may be of use in the construction of specialized subclasses.  For
 * example, parallel graph traversals can use the supplied methods to
 * avoid revisiting nodes/tasks that have already been processed.
 * (Method names for tagging are bulky in part to encourage definition
 * of methods that reflect their usage patterns.)
 * <p>
 *     方法 join() 及其变体只适合在完成依赖关系是非循环的情况下使用；也就是说，并行计算可以描述为有向无环图（DAG）。
 *     否则，由于任务之间循环等待，执行可能会遇到某种形式的死锁。
 *     不过，本框架支持其他方法和技术（例如 Phaser 、 helpQuiesce() 和 complete(V) 的使用），这些方法和技术可能有助于为非静态结构为 DAG 的问题构建自定义子类。
 *     为支持此类使用，ForkJoinTask 可以使用 setForkJoinTaskTag(short) 或 compareAndSetForkJoinTaskTag(short, short) 原子标记 short 值，并使用 getForkJoinTaskTag() 进行检查。
 *     ForkJoinTask 的实现不会将这些 protected 方法或标记用于任何目的，但在构建专门的子类时可能会用到它们。
 *     例如，并行图遍历可以使用提供的方法来避免重新访问已经处理过的节点/任务。 (用于标记的方法名称比较冗长，部分原因是为了鼓励定义能反映其使用模式的方法。)
 * </p>
 * <p>Most base support methods are {@code final}, to prevent
 * overriding of implementations that are intrinsically tied to the
 * underlying lightweight task scheduling framework.  Developers
 * creating new basic styles of fork/join processing should minimally
 * implement {@code protected} methods {@link #exec}, {@link
 * #setRawResult}, and {@link #getRawResult}, while also introducing
 * an abstract computational method that can be implemented in its
 * subclasses, possibly relying on other {@code protected} methods
 * provided by this class.
 * <p>
 *     大多数基础支持方法都是 final ，以防止覆盖与底层轻量级任务调度框架有内在联系的实现。
 *     开发人员在创建新的 fork/join 处理基本样式时，应尽量少地实现 protected 方法 exec() 、 setRawResult(V) 和 getRawResult() ，
 *     同时引入一个抽象计算方法，该方法可在其子类中实现，并可能依赖于该类提供的其他 protected 方法。
 * </p>
 * <p>ForkJoinTasks should perform relatively small amounts of
 * computation. Large tasks should be split into smaller subtasks,
 * usually via recursive decomposition. As a very rough rule of thumb,
 * a task should perform more than 100 and less than 10000 basic
 * computational steps, and should avoid indefinite looping. If tasks
 * are too big, then parallelism cannot improve throughput. If too
 * small, then memory and internal task maintenance overhead may
 * overwhelm processing.
 * <p>
 *     ForkJoinTask 应执行相对较小的计算量。大型任务应被拆分成较小的子任务，通常是通过递归分解的方式。
 *     作为一个非常粗略的经验法则，一个任务应执行 100 步以上、10000 步以下的基本计算步骤，并应避免无限循环。如果任务太大，并行化就无法提高吞吐量。
 *     如果任务太小，内存和内部任务维护开销可能会压垮处理工作。
 * </p>
 *
 * <p>This class provides {@code adapt} methods for {@link Runnable}
 * and {@link Callable}, that may be of use when mixing execution of
 * {@code ForkJoinTasks} with other kinds of tasks. When all tasks are
 * of this form, consider using a pool constructed in <em>asyncMode</em>.
 * <p>
 *     该类为 Runnable 和 Callable 提供了 adapt 方法，在将 ForkJoinTasks 与其他类型的任务混合执行时可能会用到。当所有任务都是这种形式时，可以考虑使用以 asyncMode 构建的池。
 * </p>
 * <p>ForkJoinTasks are {@code Serializable}, which enables them to be
 * used in extensions such as remote execution frameworks. It is
 * sensible to serialize tasks only before or after, but not during,
 * execution. Serialization is not relied on during execution itself.
 * <p>
 *     ForkJoinTasks 是 Serializable ，因此可以在远程执行框架等扩展中使用。只在执行前或执行后而不是执行过程中序列化任务是明智之举。执行过程本身并不依赖序列化。
 * </p>
 * @since 1.7
 * @author Doug Lea
 */
public abstract class ForkJoinTask<V> implements Future<V>, Serializable {

    /*
     * See the internal documentation of class ForkJoinPool for a
     * general implementation overview.  ForkJoinTasks are mainly
     * responsible for maintaining their "status" field amidst relays
     * to methods in ForkJoinWorkerThread and ForkJoinPool.
     *
     * The methods of this class are more-or-less layered into
     * (1) basic status maintenance
     * (2) execution and awaiting completion
     * (3) user-level methods that additionally report results.
     * This is sometimes hard to see because this file orders exported
     * methods in a way that flows well in javadocs.
     */

    /*
     * The status field holds run control status bits packed into a
     * single int to minimize footprint and to ensure atomicity (via
     * CAS).  Status is initially zero, and takes on nonnegative
     * values until completed, upon which status (anded with
     * DONE_MASK) holds value NORMAL, CANCELLED, or EXCEPTIONAL. Tasks
     * undergoing blocking waits by other threads have the SIGNAL bit
     * set.  Completion of a stolen task with SIGNAL set awakens any
     * waiters via notifyAll. Even though suboptimal for some
     * purposes, we use basic builtin wait/notify to take advantage of
     * "monitor inflation" in JVMs that we would otherwise need to
     * emulate to avoid adding further per-task bookkeeping overhead.
     * We want these monitors to be "fat", i.e., not use biasing or
     * thin-lock techniques, so use some odd coding idioms that tend
     * to avoid them, mainly by arranging that every synchronized
     * block performs a wait, notifyAll or both.
     *
     * These control bits occupy only (some of) the upper half (16
     * bits) of status field. The lower bits are used for user-defined
     * tags.
     */

    /** The run status of this task */
    volatile int status; // accessed directly by pool and workers
    static final int DONE_MASK   = 0xf0000000;  // mask out non-completion bits
    static final int NORMAL      = 0xf0000000;  // must be negative
    static final int CANCELLED   = 0xc0000000;  // must be < NORMAL
    static final int EXCEPTIONAL = 0x80000000;  // must be < CANCELLED
    static final int SIGNAL      = 0x00010000;  // must be >= 1 << 16
    static final int SMASK       = 0x0000ffff;  // short bits for tags

    /**
     * Marks completion and wakes up threads waiting to join this
     * task.
     *
     * @param completion one of NORMAL, CANCELLED, EXCEPTIONAL
     * @return completion status on exit
     */
    private int setCompletion(int completion) {
        for (int s;;) {
            if ((s = status) < 0)
                return s;
            if (U.compareAndSwapInt(this, STATUS, s, s | completion)) {
                if ((s >>> 16) != 0)
                    synchronized (this) { notifyAll(); }
                return completion;
            }
        }
    }

    /**
     * Primary execution method for stolen tasks. Unless done, calls
     * exec and records status if completed, but doesn't wait for
     * completion otherwise.
     *
     * @return status on exit from this method
     */
    final int doExec() {
        int s; boolean completed;
        if ((s = status) >= 0) {
            try {
                completed = exec();
            } catch (Throwable rex) {
                return setExceptionalCompletion(rex);
            }
            if (completed)
                s = setCompletion(NORMAL);
        }
        return s;
    }

    /**
     * If not done, sets SIGNAL status and performs Object.wait(timeout).
     * This task may or may not be done on exit. Ignores interrupts.
     *
     * @param timeout using Object.wait conventions.
     */
    final void internalWait(long timeout) {
        int s;
        if ((s = status) >= 0 && // force completer to issue notify
            U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
            synchronized (this) {
                if (status >= 0)
                    try { wait(timeout); } catch (InterruptedException ie) { }
                else
                    notifyAll();
            }
        }
    }

    /**
     * Blocks a non-worker-thread until completion.
     * @return status upon completion
     */
    private int externalAwaitDone() {
        int s = ((this instanceof CountedCompleter) ? // try helping
                 ForkJoinPool.common.externalHelpComplete(
                     (CountedCompleter<?>)this, 0) :
                 ForkJoinPool.common.tryExternalUnpush(this) ? doExec() : 0);
        if (s >= 0 && (s = status) >= 0) {
            boolean interrupted = false;
            do {
                if (U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                    synchronized (this) {
                        if (status >= 0) {
                            try {
                                wait(0L);
                            } catch (InterruptedException ie) {
                                interrupted = true;
                            }
                        }
                        else
                            notifyAll();
                    }
                }
            } while ((s = status) >= 0);
            if (interrupted)
                Thread.currentThread().interrupt();
        }
        return s;
    }

    /**
     * Blocks a non-worker-thread until completion or interruption.
     */
    private int externalInterruptibleAwaitDone() throws InterruptedException {
        int s;
        if (Thread.interrupted())
            throw new InterruptedException();
        if ((s = status) >= 0 &&
            (s = ((this instanceof CountedCompleter) ?
                  ForkJoinPool.common.externalHelpComplete(
                      (CountedCompleter<?>)this, 0) :
                  ForkJoinPool.common.tryExternalUnpush(this) ? doExec() :
                  0)) >= 0) {
            while ((s = status) >= 0) {
                if (U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                    synchronized (this) {
                        if (status >= 0)
                            wait(0L);
                        else
                            notifyAll();
                    }
                }
            }
        }
        return s;
    }

    /**
     * Implementation for join, get, quietlyJoin. Directly handles
     * only cases of already-completed, external wait, and
     * unfork+exec.  Others are relayed to ForkJoinPool.awaitJoin.
     *
     * @return status upon completion
     */
    private int doJoin() {
        int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
        return (s = status) < 0 ? s :
            ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
            (w = (wt = (ForkJoinWorkerThread)t).workQueue).
            tryUnpush(this) && (s = doExec()) < 0 ? s :
            wt.pool.awaitJoin(w, this, 0L) :
            externalAwaitDone();
    }

    /**
     * Implementation for invoke, quietlyInvoke.
     *
     * @return status upon completion
     */
    private int doInvoke() {
        int s; Thread t; ForkJoinWorkerThread wt;
        return (s = doExec()) < 0 ? s :
            ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
            (wt = (ForkJoinWorkerThread)t).pool.
            awaitJoin(wt.workQueue, this, 0L) :
            externalAwaitDone();
    }

    // Exception table support

    /**
     * Table of exceptions thrown by tasks, to enable reporting by
     * callers. Because exceptions are rare, we don't directly keep
     * them with task objects, but instead use a weak ref table.  Note
     * that cancellation exceptions don't appear in the table, but are
     * instead recorded as status values.
     *
     * Note: These statics are initialized below in static block.
     */
    private static final ExceptionNode[] exceptionTable;
    private static final ReentrantLock exceptionTableLock;
    private static final ReferenceQueue<Object> exceptionTableRefQueue;

    /**
     * Fixed capacity for exceptionTable.
     */
    private static final int EXCEPTION_MAP_CAPACITY = 32;

    /**
     * Key-value nodes for exception table.  The chained hash table
     * uses identity comparisons, full locking, and weak references
     * for keys. The table has a fixed capacity because it only
     * maintains task exceptions long enough for joiners to access
     * them, so should never become very large for sustained
     * periods. However, since we do not know when the last joiner
     * completes, we must use weak references and expunge them. We do
     * so on each operation (hence full locking). Also, some thread in
     * any ForkJoinPool will call helpExpungeStaleExceptions when its
     * pool becomes isQuiescent.
     */
    static final class ExceptionNode extends WeakReference<ForkJoinTask<?>> {
        final Throwable ex;
        ExceptionNode next;
        final long thrower;  // use id not ref to avoid weak cycles
        final int hashCode;  // store task hashCode before weak ref disappears
        ExceptionNode(ForkJoinTask<?> task, Throwable ex, ExceptionNode next) {
            super(task, exceptionTableRefQueue);
            this.ex = ex;
            this.next = next;
            this.thrower = Thread.currentThread().getId();
            this.hashCode = System.identityHashCode(task);
        }
    }

    /**
     * Records exception and sets status.
     *
     * @return status on exit
     */
    final int recordExceptionalCompletion(Throwable ex) {
        int s;
        if ((s = status) >= 0) {
            int h = System.identityHashCode(this);
            final ReentrantLock lock = exceptionTableLock;
            lock.lock();
            try {
                expungeStaleExceptions();
                ExceptionNode[] t = exceptionTable;
                int i = h & (t.length - 1);
                for (ExceptionNode e = t[i]; ; e = e.next) {
                    if (e == null) {
                        t[i] = new ExceptionNode(this, ex, t[i]);
                        break;
                    }
                    if (e.get() == this) // already present
                        break;
                }
            } finally {
                lock.unlock();
            }
            s = setCompletion(EXCEPTIONAL);
        }
        return s;
    }

    /**
     * Records exception and possibly propagates.
     *
     * @return status on exit
     */
    private int setExceptionalCompletion(Throwable ex) {
        int s = recordExceptionalCompletion(ex);
        if ((s & DONE_MASK) == EXCEPTIONAL)
            internalPropagateException(ex);
        return s;
    }

    /**
     * Hook for exception propagation support for tasks with completers.
     */
    void internalPropagateException(Throwable ex) {
    }

    /**
     * Cancels, ignoring any exceptions thrown by cancel. Used during
     * worker and pool shutdown. Cancel is spec'ed not to throw any
     * exceptions, but if it does anyway, we have no recourse during
     * shutdown, so guard against this case.
     */
    static final void cancelIgnoringExceptions(ForkJoinTask<?> t) {
        if (t != null && t.status >= 0) {
            try {
                t.cancel(false);
            } catch (Throwable ignore) {
            }
        }
    }

    /**
     * Removes exception node and clears status.
     */
    private void clearExceptionalCompletion() {
        int h = System.identityHashCode(this);
        final ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            ExceptionNode[] t = exceptionTable;
            int i = h & (t.length - 1);
            ExceptionNode e = t[i];
            ExceptionNode pred = null;
            while (e != null) {
                ExceptionNode next = e.next;
                if (e.get() == this) {
                    if (pred == null)
                        t[i] = next;
                    else
                        pred.next = next;
                    break;
                }
                pred = e;
                e = next;
            }
            expungeStaleExceptions();
            status = 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a rethrowable exception for the given task, if
     * available. To provide accurate stack traces, if the exception
     * was not thrown by the current thread, we try to create a new
     * exception of the same type as the one thrown, but with the
     * recorded exception as its cause. If there is no such
     * constructor, we instead try to use a no-arg constructor,
     * followed by initCause, to the same effect. If none of these
     * apply, or any fail due to other exceptions, we return the
     * recorded exception, which is still correct, although it may
     * contain a misleading stack trace.
     *
     * @return the exception, or null if none
     */
    private Throwable getThrowableException() {
        if ((status & DONE_MASK) != EXCEPTIONAL)
            return null;
        int h = System.identityHashCode(this);
        ExceptionNode e;
        final ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            expungeStaleExceptions();
            ExceptionNode[] t = exceptionTable;
            e = t[h & (t.length - 1)];
            while (e != null && e.get() != this)
                e = e.next;
        } finally {
            lock.unlock();
        }
        Throwable ex;
        if (e == null || (ex = e.ex) == null)
            return null;
        if (e.thrower != Thread.currentThread().getId()) {
            Class<? extends Throwable> ec = ex.getClass();
            try {
                Constructor<?> noArgCtor = null;
                Constructor<?>[] cs = ec.getConstructors();// public ctors only
                for (int i = 0; i < cs.length; ++i) {
                    Constructor<?> c = cs[i];
                    Class<?>[] ps = c.getParameterTypes();
                    if (ps.length == 0)
                        noArgCtor = c;
                    else if (ps.length == 1 && ps[0] == Throwable.class) {
                        Throwable wx = (Throwable)c.newInstance(ex);
                        return (wx == null) ? ex : wx;
                    }
                }
                if (noArgCtor != null) {
                    Throwable wx = (Throwable)(noArgCtor.newInstance());
                    if (wx != null) {
                        wx.initCause(ex);
                        return wx;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return ex;
    }

    /**
     * Poll stale refs and remove them. Call only while holding lock.
     */
    private static void expungeStaleExceptions() {
        for (Object x; (x = exceptionTableRefQueue.poll()) != null;) {
            if (x instanceof ExceptionNode) {
                int hashCode = ((ExceptionNode)x).hashCode;
                ExceptionNode[] t = exceptionTable;
                int i = hashCode & (t.length - 1);
                ExceptionNode e = t[i];
                ExceptionNode pred = null;
                while (e != null) {
                    ExceptionNode next = e.next;
                    if (e == x) {
                        if (pred == null)
                            t[i] = next;
                        else
                            pred.next = next;
                        break;
                    }
                    pred = e;
                    e = next;
                }
            }
        }
    }

    /**
     * If lock is available, poll stale refs and remove them.
     * Called from ForkJoinPool when pools become quiescent.
     */
    static final void helpExpungeStaleExceptions() {
        final ReentrantLock lock = exceptionTableLock;
        if (lock.tryLock()) {
            try {
                expungeStaleExceptions();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * A version of "sneaky throw" to relay exceptions
     */
    static void rethrow(Throwable ex) {
        if (ex != null)
            ForkJoinTask.<RuntimeException>uncheckedThrow(ex);
    }

    /**
     * The sneaky part of sneaky throw, relying on generics
     * limitations to evade compiler complaints about rethrowing
     * unchecked exceptions
     */
    @SuppressWarnings("unchecked") static <T extends Throwable>
        void uncheckedThrow(Throwable t) throws T {
        throw (T)t; // rely on vacuous cast
    }

    /**
     * Throws exception, if any, associated with the given status.
     */
    private void reportException(int s) {
        if (s == CANCELLED)
            throw new CancellationException();
        if (s == EXCEPTIONAL)
            rethrow(getThrowableException());
    }

    // public methods

    /**
     * Arranges to asynchronously execute this task in the pool the
     * current task is running in, if applicable, or using the {@link
     * ForkJoinPool#commonPool()} if not {@link #inForkJoinPool}.  While
     * it is not necessarily enforced, it is a usage error to fork a
     * task more than once unless it has completed and been
     * reinitialized.  Subsequent modifications to the state of this
     * task or any data it operates on are not necessarily
     * consistently observable by any thread other than the one
     * executing it unless preceded by a call to {@link #join} or
     * related methods, or a call to {@link #isDone} returning {@code
     * true}.
     *
     * @return {@code this}, to simplify usage
     */
    public final ForkJoinTask<V> fork() {
        Thread t;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            ((ForkJoinWorkerThread)t).workQueue.push(this);
        else
            ForkJoinPool.common.externalPush(this);
        return this;
    }

    /**
     * Returns the result of the computation when it {@link #isDone is
     * done}.  This method differs from {@link #get()} in that
     * abnormal completion results in {@code RuntimeException} or
     * {@code Error}, not {@code ExecutionException}, and that
     * interrupts of the calling thread do <em>not</em> cause the
     * method to abruptly return by throwing {@code
     * InterruptedException}.
     *
     * @return the computed result
     */
    public final V join() {
        int s;
        if ((s = doJoin() & DONE_MASK) != NORMAL)
            reportException(s);
        return getRawResult();
    }

    /**
     * Commences performing this task, awaits its completion if
     * necessary, and returns its result, or throws an (unchecked)
     * {@code RuntimeException} or {@code Error} if the underlying
     * computation did so.
     *
     * @return the computed result
     */
    public final V invoke() {
        int s;
        if ((s = doInvoke() & DONE_MASK) != NORMAL)
            reportException(s);
        return getRawResult();
    }

    /**
     * Forks the given tasks, returning when {@code isDone} holds for
     * each task or an (unchecked) exception is encountered, in which
     * case the exception is rethrown. If more than one task
     * encounters an exception, then this method throws any one of
     * these exceptions. If any task encounters an exception, the
     * other may be cancelled. However, the execution status of
     * individual tasks is not guaranteed upon exceptional return. The
     * status of each task may be obtained using {@link
     * #getException()} and related methods to check if they have been
     * cancelled, completed normally or exceptionally, or left
     * unprocessed.
     *
     * @param t1 the first task
     * @param t2 the second task
     * @throws NullPointerException if any task is null
     */
    public static void invokeAll(ForkJoinTask<?> t1, ForkJoinTask<?> t2) {
        int s1, s2;
        t2.fork();
        if ((s1 = t1.doInvoke() & DONE_MASK) != NORMAL)
            t1.reportException(s1);
        if ((s2 = t2.doJoin() & DONE_MASK) != NORMAL)
            t2.reportException(s2);
    }

    /**
     * Forks the given tasks, returning when {@code isDone} holds for
     * each task or an (unchecked) exception is encountered, in which
     * case the exception is rethrown. If more than one task
     * encounters an exception, then this method throws any one of
     * these exceptions. If any task encounters an exception, others
     * may be cancelled. However, the execution status of individual
     * tasks is not guaranteed upon exceptional return. The status of
     * each task may be obtained using {@link #getException()} and
     * related methods to check if they have been cancelled, completed
     * normally or exceptionally, or left unprocessed.
     *
     * @param tasks the tasks
     * @throws NullPointerException if any task is null
     */
    public static void invokeAll(ForkJoinTask<?>... tasks) {
        Throwable ex = null;
        int last = tasks.length - 1;
        for (int i = last; i >= 0; --i) {
            ForkJoinTask<?> t = tasks[i];
            if (t == null) {
                if (ex == null)
                    ex = new NullPointerException();
            }
            else if (i != 0)
                t.fork();
            else if (t.doInvoke() < NORMAL && ex == null)
                ex = t.getException();
        }
        for (int i = 1; i <= last; ++i) {
            ForkJoinTask<?> t = tasks[i];
            if (t != null) {
                if (ex != null)
                    t.cancel(false);
                else if (t.doJoin() < NORMAL)
                    ex = t.getException();
            }
        }
        if (ex != null)
            rethrow(ex);
    }

    /**
     * Forks all tasks in the specified collection, returning when
     * {@code isDone} holds for each task or an (unchecked) exception
     * is encountered, in which case the exception is rethrown. If
     * more than one task encounters an exception, then this method
     * throws any one of these exceptions. If any task encounters an
     * exception, others may be cancelled. However, the execution
     * status of individual tasks is not guaranteed upon exceptional
     * return. The status of each task may be obtained using {@link
     * #getException()} and related methods to check if they have been
     * cancelled, completed normally or exceptionally, or left
     * unprocessed.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     * @return the tasks argument, to simplify usage
     * @throws NullPointerException if tasks or any element are null
     */
    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        if (!(tasks instanceof RandomAccess) || !(tasks instanceof List<?>)) {
            invokeAll(tasks.toArray(new ForkJoinTask<?>[tasks.size()]));
            return tasks;
        }
        @SuppressWarnings("unchecked")
        List<? extends ForkJoinTask<?>> ts =
            (List<? extends ForkJoinTask<?>>) tasks;
        Throwable ex = null;
        int last = ts.size() - 1;
        for (int i = last; i >= 0; --i) {
            ForkJoinTask<?> t = ts.get(i);
            if (t == null) {
                if (ex == null)
                    ex = new NullPointerException();
            }
            else if (i != 0)
                t.fork();
            else if (t.doInvoke() < NORMAL && ex == null)
                ex = t.getException();
        }
        for (int i = 1; i <= last; ++i) {
            ForkJoinTask<?> t = ts.get(i);
            if (t != null) {
                if (ex != null)
                    t.cancel(false);
                else if (t.doJoin() < NORMAL)
                    ex = t.getException();
            }
        }
        if (ex != null)
            rethrow(ex);
        return tasks;
    }

    /**
     * Attempts to cancel execution of this task. This attempt will
     * fail if the task has already completed or could not be
     * cancelled for some other reason. If successful, and this task
     * has not started when {@code cancel} is called, execution of
     * this task is suppressed. After this method returns
     * successfully, unless there is an intervening call to {@link
     * #reinitialize}, subsequent calls to {@link #isCancelled},
     * {@link #isDone}, and {@code cancel} will return {@code true}
     * and calls to {@link #join} and related methods will result in
     * {@code CancellationException}.
     *
     * <p>This method may be overridden in subclasses, but if so, must
     * still ensure that these properties hold. In particular, the
     * {@code cancel} method itself must not throw exceptions.
     *
     * <p>This method is designed to be invoked by <em>other</em>
     * tasks. To terminate the current task, you can just return or
     * throw an unchecked exception from its computation method, or
     * invoke {@link #completeExceptionally(Throwable)}.
     *
     * @param mayInterruptIfRunning this value has no effect in the
     * default implementation because interrupts are not used to
     * control cancellation.
     *
     * @return {@code true} if this task is now cancelled
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return (setCompletion(CANCELLED) & DONE_MASK) == CANCELLED;
    }

    public final boolean isDone() {
        return status < 0;
    }

    public final boolean isCancelled() {
        return (status & DONE_MASK) == CANCELLED;
    }

    /**
     * Returns {@code true} if this task threw an exception or was cancelled.
     *
     * @return {@code true} if this task threw an exception or was cancelled
     */
    public final boolean isCompletedAbnormally() {
        return status < NORMAL;
    }

    /**
     * Returns {@code true} if this task completed without throwing an
     * exception and was not cancelled.
     *
     * @return {@code true} if this task completed without throwing an
     * exception and was not cancelled
     */
    public final boolean isCompletedNormally() {
        return (status & DONE_MASK) == NORMAL;
    }

    /**
     * Returns the exception thrown by the base computation, or a
     * {@code CancellationException} if cancelled, or {@code null} if
     * none or if the method has not yet completed.
     *
     * @return the exception, or {@code null} if none
     */
    public final Throwable getException() {
        int s = status & DONE_MASK;
        return ((s >= NORMAL)    ? null :
                (s == CANCELLED) ? new CancellationException() :
                getThrowableException());
    }

    /**
     * Completes this task abnormally, and if not already aborted or
     * cancelled, causes it to throw the given exception upon
     * {@code join} and related operations. This method may be used
     * to induce exceptions in asynchronous tasks, or to force
     * completion of tasks that would not otherwise complete.  Its use
     * in other situations is discouraged.  This method is
     * overridable, but overridden versions must invoke {@code super}
     * implementation to maintain guarantees.
     *
     * @param ex the exception to throw. If this exception is not a
     * {@code RuntimeException} or {@code Error}, the actual exception
     * thrown will be a {@code RuntimeException} with cause {@code ex}.
     */
    public void completeExceptionally(Throwable ex) {
        setExceptionalCompletion((ex instanceof RuntimeException) ||
                                 (ex instanceof Error) ? ex :
                                 new RuntimeException(ex));
    }

    /**
     * Completes this task, and if not already aborted or cancelled,
     * returning the given value as the result of subsequent
     * invocations of {@code join} and related operations. This method
     * may be used to provide results for asynchronous tasks, or to
     * provide alternative handling for tasks that would not otherwise
     * complete normally. Its use in other situations is
     * discouraged. This method is overridable, but overridden
     * versions must invoke {@code super} implementation to maintain
     * guarantees.
     *
     * @param value the result value for this task
     */
    public void complete(V value) {
        try {
            setRawResult(value);
        } catch (Throwable rex) {
            setExceptionalCompletion(rex);
            return;
        }
        setCompletion(NORMAL);
    }

    /**
     * Completes this task normally without setting a value. The most
     * recent value established by {@link #setRawResult} (or {@code
     * null} by default) will be returned as the result of subsequent
     * invocations of {@code join} and related operations.
     *
     * @since 1.8
     */
    public final void quietlyComplete() {
        setCompletion(NORMAL);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread is not a
     * member of a ForkJoinPool and was interrupted while waiting
     */
    public final V get() throws InterruptedException, ExecutionException {
        int s = (Thread.currentThread() instanceof ForkJoinWorkerThread) ?
            doJoin() : externalInterruptibleAwaitDone();
        Throwable ex;
        if ((s &= DONE_MASK) == CANCELLED)
            throw new CancellationException();
        if (s == EXCEPTIONAL && (ex = getThrowableException()) != null)
            throw new ExecutionException(ex);
        return getRawResult();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread is not a
     * member of a ForkJoinPool and was interrupted while waiting
     * @throws TimeoutException if the wait timed out
     */
    public final V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        int s;
        long nanos = unit.toNanos(timeout);
        if (Thread.interrupted())
            throw new InterruptedException();
        if ((s = status) >= 0 && nanos > 0L) {
            long d = System.nanoTime() + nanos;
            long deadline = (d == 0L) ? 1L : d; // avoid 0
            Thread t = Thread.currentThread();
            if (t instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
                s = wt.pool.awaitJoin(wt.workQueue, this, deadline);
            }
            else if ((s = ((this instanceof CountedCompleter) ?
                           ForkJoinPool.common.externalHelpComplete(
                               (CountedCompleter<?>)this, 0) :
                           ForkJoinPool.common.tryExternalUnpush(this) ?
                           doExec() : 0)) >= 0) {
                long ns, ms; // measure in nanosecs, but wait in millisecs
                while ((s = status) >= 0 &&
                       (ns = deadline - System.nanoTime()) > 0L) {
                    if ((ms = TimeUnit.NANOSECONDS.toMillis(ns)) > 0L &&
                        U.compareAndSwapInt(this, STATUS, s, s | SIGNAL)) {
                        synchronized (this) {
                            if (status >= 0)
                                wait(ms); // OK to throw InterruptedException
                            else
                                notifyAll();
                        }
                    }
                }
            }
        }
        if (s >= 0)
            s = status;
        if ((s &= DONE_MASK) != NORMAL) {
            Throwable ex;
            if (s == CANCELLED)
                throw new CancellationException();
            if (s != EXCEPTIONAL)
                throw new TimeoutException();
            if ((ex = getThrowableException()) != null)
                throw new ExecutionException(ex);
        }
        return getRawResult();
    }

    /**
     * Joins this task, without returning its result or throwing its
     * exception. This method may be useful when processing
     * collections of tasks when some have been cancelled or otherwise
     * known to have aborted.
     */
    public final void quietlyJoin() {
        doJoin();
    }

    /**
     * Commences performing this task and awaits its completion if
     * necessary, without returning its result or throwing its
     * exception.
     */
    public final void quietlyInvoke() {
        doInvoke();
    }

    /**
     * Possibly executes tasks until the pool hosting the current task
     * {@link ForkJoinPool#isQuiescent is quiescent}. This method may
     * be of use in designs in which many tasks are forked, but none
     * are explicitly joined, instead executing them until all are
     * processed.
     */
    public static void helpQuiesce() {
        Thread t;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread)t;
            wt.pool.helpQuiescePool(wt.workQueue);
        }
        else
            ForkJoinPool.quiesceCommonPool();
    }

    /**
     * Resets the internal bookkeeping state of this task, allowing a
     * subsequent {@code fork}. This method allows repeated reuse of
     * this task, but only if reuse occurs when this task has either
     * never been forked, or has been forked, then completed and all
     * outstanding joins of this task have also completed. Effects
     * under any other usage conditions are not guaranteed.
     * This method may be useful when executing
     * pre-constructed trees of subtasks in loops.
     *
     * <p>Upon completion of this method, {@code isDone()} reports
     * {@code false}, and {@code getException()} reports {@code
     * null}. However, the value returned by {@code getRawResult} is
     * unaffected. To clear this value, you can invoke {@code
     * setRawResult(null)}.
     */
    public void reinitialize() {
        if ((status & DONE_MASK) == EXCEPTIONAL)
            clearExceptionalCompletion();
        else
            status = 0;
    }

    /**
     * Returns the pool hosting the current task execution, or null
     * if this task is executing outside of any ForkJoinPool.
     *
     * @see #inForkJoinPool
     * @return the pool, or {@code null} if none
     */
    public static ForkJoinPool getPool() {
        Thread t = Thread.currentThread();
        return (t instanceof ForkJoinWorkerThread) ?
            ((ForkJoinWorkerThread) t).pool : null;
    }

    /**
     * Returns {@code true} if the current thread is a {@link
     * ForkJoinWorkerThread} executing as a ForkJoinPool computation.
     *
     * @return {@code true} if the current thread is a {@link
     * ForkJoinWorkerThread} executing as a ForkJoinPool computation,
     * or {@code false} otherwise
     */
    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread;
    }

    /**
     * Tries to unschedule this task for execution. This method will
     * typically (but is not guaranteed to) succeed if this task is
     * the most recently forked task by the current thread, and has
     * not commenced executing in another thread.  This method may be
     * useful when arranging alternative local processing of tasks
     * that could have been, but were not, stolen.
     *
     * @return {@code true} if unforked
     */
    public boolean tryUnfork() {
        Thread t;
        return (((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
                ((ForkJoinWorkerThread)t).workQueue.tryUnpush(this) :
                ForkJoinPool.common.tryExternalUnpush(this));
    }

    /**
     * Returns an estimate of the number of tasks that have been
     * forked by the current worker thread but not yet executed. This
     * value may be useful for heuristic decisions about whether to
     * fork other tasks.
     *
     * @return the number of tasks
     */
    public static int getQueuedTaskCount() {
        Thread t; ForkJoinPool.WorkQueue q;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            q = ((ForkJoinWorkerThread)t).workQueue;
        else
            q = ForkJoinPool.commonSubmitterQueue();
        return (q == null) ? 0 : q.queueSize();
    }

    /**
     * Returns an estimate of how many more locally queued tasks are
     * held by the current worker thread than there are other worker
     * threads that might steal them, or zero if this thread is not
     * operating in a ForkJoinPool. This value may be useful for
     * heuristic decisions about whether to fork other tasks. In many
     * usages of ForkJoinTasks, at steady state, each worker should
     * aim to maintain a small constant surplus (for example, 3) of
     * tasks, and to process computations locally if this threshold is
     * exceeded.
     *
     * @return the surplus number of tasks, which may be negative
     */
    public static int getSurplusQueuedTaskCount() {
        return ForkJoinPool.getSurplusQueuedTaskCount();
    }

    // Extension methods

    /**
     * Returns the result that would be returned by {@link #join}, even
     * if this task completed abnormally, or {@code null} if this task
     * is not known to have been completed.  This method is designed
     * to aid debugging, as well as to support extensions. Its use in
     * any other context is discouraged.
     *
     * @return the result, or {@code null} if not completed
     */
    public abstract V getRawResult();

    /**
     * Forces the given value to be returned as a result.  This method
     * is designed to support extensions, and should not in general be
     * called otherwise.
     *
     * @param value the value
     */
    protected abstract void setRawResult(V value);

    /**
     * Immediately performs the base action of this task and returns
     * true if, upon return from this method, this task is guaranteed
     * to have completed normally. This method may return false
     * otherwise, to indicate that this task is not necessarily
     * complete (or is not known to be complete), for example in
     * asynchronous actions that require explicit invocations of
     * completion methods. This method may also throw an (unchecked)
     * exception to indicate abnormal exit. This method is designed to
     * support extensions, and should not in general be called
     * otherwise.
     *
     * @return {@code true} if this task is known to have completed normally
     */
    protected abstract boolean exec();

    /**
     * Returns, but does not unschedule or execute, a task queued by
     * the current thread but not yet executed, if one is immediately
     * available. There is no guarantee that this task will actually
     * be polled or executed next. Conversely, this method may return
     * null even if a task exists but cannot be accessed without
     * contention with other threads.  This method is designed
     * primarily to support extensions, and is unlikely to be useful
     * otherwise.
     *
     * @return the next task, or {@code null} if none are available
     */
    protected static ForkJoinTask<?> peekNextLocalTask() {
        Thread t; ForkJoinPool.WorkQueue q;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            q = ((ForkJoinWorkerThread)t).workQueue;
        else
            q = ForkJoinPool.commonSubmitterQueue();
        return (q == null) ? null : q.peek();
    }

    /**
     * Unschedules and returns, without executing, the next task
     * queued by the current thread but not yet executed, if the
     * current thread is operating in a ForkJoinPool.  This method is
     * designed primarily to support extensions, and is unlikely to be
     * useful otherwise.
     *
     * @return the next task, or {@code null} if none are available
     */
    protected static ForkJoinTask<?> pollNextLocalTask() {
        Thread t;
        return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
            ((ForkJoinWorkerThread)t).workQueue.nextLocalTask() :
            null;
    }

    /**
     * If the current thread is operating in a ForkJoinPool,
     * unschedules and returns, without executing, the next task
     * queued by the current thread but not yet executed, if one is
     * available, or if not available, a task that was forked by some
     * other thread, if available. Availability may be transient, so a
     * {@code null} result does not necessarily imply quiescence of
     * the pool this task is operating in.  This method is designed
     * primarily to support extensions, and is unlikely to be useful
     * otherwise.
     *
     * @return a task, or {@code null} if none are available
     */
    protected static ForkJoinTask<?> pollTask() {
        Thread t; ForkJoinWorkerThread wt;
        return ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
            (wt = (ForkJoinWorkerThread)t).pool.nextTaskFor(wt.workQueue) :
            null;
    }

    // tag operations

    /**
     * Returns the tag for this task.
     *
     * @return the tag for this task
     * @since 1.8
     */
    public final short getForkJoinTaskTag() {
        return (short)status;
    }

    /**
     * Atomically sets the tag value for this task.
     *
     * @param tag the tag value
     * @return the previous value of the tag
     * @since 1.8
     */
    public final short setForkJoinTaskTag(short tag) {
        for (int s;;) {
            if (U.compareAndSwapInt(this, STATUS, s = status,
                                    (s & ~SMASK) | (tag & SMASK)))
                return (short)s;
        }
    }

    /**
     * Atomically conditionally sets the tag value for this task.
     * Among other applications, tags can be used as visit markers
     * in tasks operating on graphs, as in methods that check: {@code
     * if (task.compareAndSetForkJoinTaskTag((short)0, (short)1))}
     * before processing, otherwise exiting because the node has
     * already been visited.
     *
     * @param e the expected tag value
     * @param tag the new tag value
     * @return {@code true} if successful; i.e., the current value was
     * equal to e and is now tag.
     * @since 1.8
     */
    public final boolean compareAndSetForkJoinTaskTag(short e, short tag) {
        for (int s;;) {
            if ((short)(s = status) != e)
                return false;
            if (U.compareAndSwapInt(this, STATUS, s,
                                    (s & ~SMASK) | (tag & SMASK)))
                return true;
        }
    }

    /**
     * Adaptor for Runnables. This implements RunnableFuture
     * to be compliant with AbstractExecutorService constraints
     * when used in ForkJoinPool.
     */
    static final class AdaptedRunnable<T> extends ForkJoinTask<T>
        implements RunnableFuture<T> {
        final Runnable runnable;
        T result;
        AdaptedRunnable(Runnable runnable, T result) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
            this.result = result; // OK to set this even before completion
        }
        public final T getRawResult() { return result; }
        public final void setRawResult(T v) { result = v; }
        public final boolean exec() { runnable.run(); return true; }
        public final void run() { invoke(); }
        private static final long serialVersionUID = 5232453952276885070L;
    }

    /**
     * Adaptor for Runnables without results
     */
    static final class AdaptedRunnableAction extends ForkJoinTask<Void>
        implements RunnableFuture<Void> {
        final Runnable runnable;
        AdaptedRunnableAction(Runnable runnable) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
        }
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) { }
        public final boolean exec() { runnable.run(); return true; }
        public final void run() { invoke(); }
        private static final long serialVersionUID = 5232453952276885070L;
    }

    /**
     * Adaptor for Runnables in which failure forces worker exception
     */
    static final class RunnableExecuteAction extends ForkJoinTask<Void> {
        final Runnable runnable;
        RunnableExecuteAction(Runnable runnable) {
            if (runnable == null) throw new NullPointerException();
            this.runnable = runnable;
        }
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) { }
        public final boolean exec() { runnable.run(); return true; }
        void internalPropagateException(Throwable ex) {
            rethrow(ex); // rethrow outside exec() catches.
        }
        private static final long serialVersionUID = 5232453952276885070L;
    }

    /**
     * Adaptor for Callables
     */
    static final class AdaptedCallable<T> extends ForkJoinTask<T>
        implements RunnableFuture<T> {
        final Callable<? extends T> callable;
        T result;
        AdaptedCallable(Callable<? extends T> callable) {
            if (callable == null) throw new NullPointerException();
            this.callable = callable;
        }
        public final T getRawResult() { return result; }
        public final void setRawResult(T v) { result = v; }
        public final boolean exec() {
            try {
                result = callable.call();
                return true;
            } catch (Error err) {
                throw err;
            } catch (RuntimeException rex) {
                throw rex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        public final void run() { invoke(); }
        private static final long serialVersionUID = 2838392045355241008L;
    }

    /**
     * Returns a new {@code ForkJoinTask} that performs the {@code run}
     * method of the given {@code Runnable} as its action, and returns
     * a null result upon {@link #join}.
     *
     * @param runnable the runnable action
     * @return the task
     */
    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnableAction(runnable);
    }

    /**
     * Returns a new {@code ForkJoinTask} that performs the {@code run}
     * method of the given {@code Runnable} as its action, and returns
     * the given result upon {@link #join}.
     *
     * @param runnable the runnable action
     * @param result the result upon completion
     * @param <T> the type of the result
     * @return the task
     */
    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable<T>(runnable, result);
    }

    /**
     * Returns a new {@code ForkJoinTask} that performs the {@code call}
     * method of the given {@code Callable} as its action, and returns
     * its result upon {@link #join}, translating any checked exceptions
     * encountered into {@code RuntimeException}.
     *
     * @param callable the callable action
     * @param <T> the type of the callable's result
     * @return the task
     */
    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable<T>(callable);
    }

    // Serialization support

    private static final long serialVersionUID = -7721805057305804111L;

    /**
     * Saves this task to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData the current run status and the exception thrown
     * during execution, or {@code null} if none
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        s.defaultWriteObject();
        s.writeObject(getException());
    }

    /**
     * Reconstitutes this task from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        Object ex = s.readObject();
        if (ex != null)
            setExceptionalCompletion((Throwable)ex);
    }

    // sum.misc.Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long STATUS;

    static {
        exceptionTableLock = new ReentrantLock();
        exceptionTableRefQueue = new ReferenceQueue<Object>();
        exceptionTable = new ExceptionNode[EXCEPTION_MAP_CAPACITY];
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ForkJoinTask.class;
            STATUS = U.objectFieldOffset
                (k.getDeclaredField("status"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
