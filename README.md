# jdk1.8.0_111
JDK 1.8 源码注释

[Thread](src/java/lang/Thread.java)  
[ThreadLocal](src/java/lang/ThreadLocal.java)

`InheritableThreadLocal`跟`ThreadLocal`差不多，关键点在`Thread#init`方法中会对`Thread`中的实例变量`inheritableThreadLocals`进行赋值操作，将父线程的本地变量复制到子线程中。

[LongAdder](src/java/util/concurrent/atomic/LongAdder.java)  
[Striped64](src/java/util/concurrent/atomic/Striped64.java)
[ArrayList](src/java/util/ArrayList.java)  
[LinkedList](src/java/util/LinkedList.java)  
[HashMap](src/java/util/HashMap.java)  
[LinkedHashMap](src/java/util/LinkedHashMap.java)  
[ReentrantLock](src/java/util/concurrent/locks/ReentrantLock.java)  
[ReentrantReadWriteLock](src/java/util/concurrent/locks/ReentrantReadWriteLock.java)    
[CopyOnWriteArrayList](src/java/util/concurrent/CopyOnWriteArrayList.java)  
[AtomicLong](src/java/util/concurrent/atomic/AtomicLong.java)  
[ConcurrentHashMap](src/java/util/concurrent/ConcurrentHashMap.java)  
[AtomicInteger](src/java/util/concurrent/atomic/AtomicInteger.java)  
[CountDownLatch](src/java/util/concurrent/CountDownLatch.java)  
[CyclicBarrier](src/java/util/concurrent/CyclicBarrier.java)  
[Semaphore](src/java/util/concurrent/Semaphore.java)  