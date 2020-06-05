# jdk1.8.0_111
JDK 1.8 源码注释



- [Thread](src/java/lang/Thread.java)  

- [ThreadLocal](src/java/lang/ThreadLocal.java)  

- [InheritableThreadLocal](src/java/lang/InheritableThreadLocal.java)
  
  `InheritableThreadLocal`跟`ThreadLocal`差不多，关键点在`Thread#init`方法中会对`Thread`中的实例变量`inheritableThreadLocals`进行赋值操作，将父线程的本地变量复制到子线程中

- [Striped64](src/java/util/concurrent/atomic/Striped64.java)  

- [ArrayList](src/java/util/ArrayList.java)  

- [LinkedList](src/java/util/LinkedList.java)  

- [HashMap](src/java/util/HashMap.java)  

- [LinkedHashMap](src/java/util/LinkedHashMap.java)  

- [ReentrantLock](src/java/util/concurrent/locks/ReentrantLock.java)  

- [ReentrantReadWriteLock](src/java/util/concurrent/locks/ReentrantReadWriteLock.java)  

- [CopyOnWriteArrayList](src/java/util/concurrent/CopyOnWriteArrayList.java)  

- [AtomicLong](src/java/util/concurrent/atomic/AtomicLong.java)  

- [ConcurrentHashMap](src/java/util/concurrent/ConcurrentHashMap.java)  

- [AtomicInteger](src/java/util/concurrent/atomic/AtomicInteger.java)  

- [CountDownLatch](src/java/util/concurrent/CountDownLatch.java)  

- [CyclicBarrier](src/java/util/concurrent/CyclicBarrier.java)  

- [Semaphore](src/java/util/concurrent/Semaphore.java)  

- [ServiceLoader](src/java/util/ServiceLoader.java)

  [SPI使用示例](http://dubbo.apache.org/zh-cn/docs/source_code_guide/dubbo-spi.html)

  - 首先通过`ServiceLoader.load`方法生成一个`ServiceLoader`实例
  - 然后在遍历的过程中去解析文件中的内容，去生成具体的实现类
  - 然后再去调用具体的实现方法

  只有在用到某个实现类的时候才会去解析文件中的内容，而且是全部解析，即并不是我们需要的实现类也会解析。而且需要遍历才能找到我们想要的那个实现类。

  文件中的内容被加载一次，然后被缓存起来，下次直接取就行。

  如果想要重新加载，可以调用`reload()`方法

  



