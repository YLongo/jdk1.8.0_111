# jdk1.8.0_111
JDK 1.8 源码注释

[Thread](src/java/lang/Thread.java)

[ThreadLocal](src/java/lang/ThreadLocal.java)

`InheritableThreadLocal`跟`ThreadLocal`差不多，关键点在`Thread#init`方法中会对`Thread`中的实例变量`inheritableThreadLocals`进行赋值操作，将父线程的本地变量复制到子线程中。

