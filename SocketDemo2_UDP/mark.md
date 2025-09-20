`DatagramSocket`既是客户端又是服务端，用来发送和接收消息

不同于TCP,不存在与socket的API中

**DatagramSocket**(int port) 指定监听端口，接收数据的端口

**DatagramSocket**(int port, InetAddress localAddr) 指定ip指定port监听的实例


**receive**(DatagramPacket packet) 接收数据
**send**(DatagramPacket packet) 发送数据

**DatagramPacket**
> 用于处理报文,将byte数组、目标地址、目标端口等数据包装成报文或者将报文拆卸成byte数组:

**API**
>ip与port仅仅在发送时有效
- DatagramPacket(byte[] buf, int offset, int length, InetAddress
address, int port)
- setData(bytel buf, int offset, int length)
- setData(byte[] buf)
- setLength(int length)
- getData() getOffset() getLength()

----------------------------------

**单播，广播，多播**

IP地址分为ABCDE类
- A类：子网掩码255.0.0.0
- B类：子网掩码255.255.0.0
- C类：子网掩码255.255.255.0
- D类：预留给多播
- E类：实验研究

##### **广播地址**

255.255.255.255为受限广播地址

C网广播地址一般为:XXX.XXX.XXX.255 (192.168.1.255)
