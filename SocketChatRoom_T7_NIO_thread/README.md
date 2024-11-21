### **简易聊天**

客户端将消息发给服务端，服务端等待转发给其他客户端。

每个客户端都需要服务器进行转发，双通等待。

双通：客户端发送数据到服务器接收通道 + 服务器回送消息发送通道。

每条通道因为堵塞只能使用异步线程实现。


##### 分析：

> 一个客户端：双通， > 2条线程
> 
>n个客户端，2n条线程
>
>服务器实际线程数量： 2n+


客户端数量较多时，
CPU消耗50%， 内存占用很高，线程数量爆发。


#### 优化：

1. 减少线程数量
2. 增加线程执行繁忙状态，减少线程空闲等待时间与线程切换时间
3. 内存，加客户端Buffer复用机制


#### 阻塞IO和非阻塞IO
NIO:
* Buffer 缓冲区，用于数据处理的基础单元，客户端发送与接收数据都需要通过Buffer转发进行。
* Channel 通道，类似于流，但偏向于数据的流通多样性。
* Selectors 选择器，处理客户端所有事件的分发器，用于管理事件。

* Buffer包括：
> ByteBuffer,CharBuffer,ShortBuffer,IntBuffer,LongBuffer,FloatBuffer,DoubleBuffer

* Channel包括：
> FileChannel,SocketChannel,DatagramChannel

Buffer为NIO按块操作提供了基础，数据按“块”传输。一个Buffer代表一块数据。

Buffer中其实是维护了一个数组。

可以从通道读取数据，也可以输出数据到通道，按块进行操作。

NIO可以并发异步读写数据。

##### NIO的API

Selector:向Selector注册一个事件，对应Channel的状态
Channel:Channel状态变化时，触发注册的事件
Buffer:

注册事件：
* SelectionKey.OP_CONNECT 连接就绪
* SelectionKey.OP_ACCEPT  接受就绪
* SelectionKey.OP_READ  读就绪
* SelectionKey.OP_WRITE  写就绪

Selector使用流程：
* open()方法开启一个选择器，给选择器注册需要关注的事件
* register() 将一个Channel注册到选择器，当选择器触发对应关注事件时，回调到Channel中，处理相关数据。
* select()/selectNow() 一个通道Channel，处理一个当前可用，待处理的通道数据。是阻塞操作。
* selectedKeys()拿到当前就绪的通道。

FileChannel不能用于Selector,因为FileChannel不能切换为非阻塞模式。


##### 【异步线程优化】- 监听与数据处理线程分离










