package com.serendipity.server.registry;

import com.serendipity.server.registry.handler.RegistryHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * 
 * @author serendipity 2019年6月18日
 */
public class RpcRegistry {
	private int port;// ip端口

	public RpcRegistry(int port) {
		this.port = port;
	}

	public void startRpcServer() {
		System.out.println("开始启动 RpcServer");
		// Group：群组，Loop：循环，Event：事件，这几个东西联在一起。
		// Netty内部都是通过线程在处理各种数据，EventLoopGroup就是用来管理调度他们的，注册Channel，管理他们的生命周期。
		// NioEventLoopGroup是一个处理I/O操作的多线程事件循环
		// bossGroup作为boss,接收传入连接
		// 因为bossGroup仅接收客户端连接，不做复杂的逻辑处理，为了尽可能减少资源的占用，取值越小越好
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		// workerGroup作为worker，处理boss接收的连接的流量和将接收的连接注册进入这个worker
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			// ServerBootstrap负责建立服务端
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					// 指定使用NioServerSocketChannel产生一个Channel用来接收连接
					.channel(NioServerSocketChannel.class)
					// ChannelInitializer用于配置一个新的Channel
					// 用于向你的Channel当中添加ChannelInboundHandler的实现
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							// ChannelPipeline用于存放管理ChannelHandel
							// ChannelHandler用于处理请求响应的业务逻辑相关代码
							ChannelPipeline pipeline = ch.pipeline();
							// 自定义协议解码器
							/**
							 * 入参有5个，分别解释如下 maxFrameLength：框架的最大长度。如果帧的长度大于此值，
							 * 则将抛出TooLongFrameException。
							 * lengthFieldOffset：长度字段的偏移量：即对应的长度字段在整个消息数据中得位置
							 * lengthFieldLength：长度字段的长度。如：长度字段是int型表示，那么这个值就是4（
							 * long型就是8） lengthAdjustment：要添加到长度字段值的补偿值
							 * initialBytesToStrip：从解码帧中去除的第一个字节数
							 */
							pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
							// 自定义协议编码器
							pipeline.addLast(new LengthFieldPrepender(4));
							// 对象参数类型编码器
							pipeline.addLast("encoder", new ObjectEncoder());
							// 对象参数类型解码器
							pipeline.addLast("decoder",
									new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
							pipeline.addLast(new RegistryHandler());

						}
					})
					// 对Channel进行一些配置
					// 注意以下是socket的标准参数
					// BACKLOG用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度。如果未设置或所设置的值小于1，Java将使用默认值50。
					// Option是为了NioServerSocketChannel设置的，用来接收传入连接的
					.option(ChannelOption.SO_BACKLOG, 128)
					// 是否启用心跳保活机制。在双方TCP套接字建立连接后（即都进入ESTABLISHED状态）并且在两个小时左右上层没有任何数据传输的情况下，这套机制才会被激活。
					// childOption是用来给父级ServerChannel之下的Channels设置参数的
					.childOption(ChannelOption.SO_KEEPALIVE, true);
			// Bind and start to accept incoming connections
			ChannelFuture f = b.bind(port).sync();
			System.out.println("完成启动 RpcServer,等待请求");
			// sync()会同步等待连接操作结果，用户线程将在此wait()，直到连接操作完成之后，线程被notify(),用户代码继续执行
			// closeFuture()当Channel关闭时返回一个ChannelFuture,用于链路检测
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// 资源释放
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		new RpcRegistry(8080).startRpcServer();
	}
}
