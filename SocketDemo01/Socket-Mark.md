# Socket网络编程

### UDP用户数据报协议

1. ##### 为什么不可靠？

> 它一旦把应用程序发给网络层的数据发送出去，就不保留数据备份
>
> UDP在IP数据报的头部仅仅加入了复用和数据校验(字段)
>
> 发送端生产数据，接收端从网络中抓取数据
>
> 结构简单、无校验、速度快、容易丢包、可广播

2. ##### UDP包最大长度

   > 16位->2字节 存储长度信息
   >
   > 2^16-1= 64K-1= 65536-1= 65535
   >
   > 自身协议占用:32+32位= 64位=8字节
   >
   > 65535-8 = 65507 byte

​      所以，UDP包最大长度是65507字节

3. ##### UDP核心API

- ​      `DatagramSocket` 

> 用于接收与发送UDP的类
>
>  负责发送某一个UDP包，或者接收UDP包
>
>  不同于TCP,UDP并没有合并到Socket API中

- ​     `DatagramPacket` 

>  用于处理报文
>
>   将byte数组、目标地址、目标端口等数据包装成报文或者将报文拆卸成byte数组
>
>   是UDP的发送和接收实体

```java
DatagramPacket(byte[] buf, int offset, int length, InetAddress
address, int port)
// 前面3个参数指定buf的使用区间
// 后面2个参数指定目标机器地址与端口,发送时有效

DatagramPacket(byte[] buf int length, SocketAddress
address)
// 前面3个参数指定buf的使用区间
// SocketAddress相当于InetAddress+Port

setData(byte[] buf int offset, int length);
setData(byte[] buf);
setLength(int length);
getData();
getOffset();
getLength();
setAddress(InetAddress iaddr);
setPort(int iport);
getAddress();
getPort();
setSocketAddress(SocketAddress address);
getSocketAddress();


```

4. ##### TCP核心API

   ```java
   Socket(); // 创建一个客户端Socket
   ServerSocket(); // 创建一个服务端Socket
   bind(); //绑定一个Socket到一个本地地址和端口上
   connect(); // 连接到远程套接字
   accept(); // 接受一个新的连接
   write(); // 把数据写入到Socket输出流
   read(); // 从Socket输入流读取数据
   ```

   
