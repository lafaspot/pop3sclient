package com.lafaspot.pop.client;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import org.testng.annotations.Test;

import com.lafaspot.pop.exception.PopException;
import com.lafaspot.pop.exception.PopException.Type;
import com.lafaspot.pop.netty.PopMessageDecoder;
import com.lafaspot.pop.session.PopInactivityHandler;
import com.lafaspot.pop.session.PopSession;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

public class SniTestClient {
	

	
	@Test (enabled=false)
	public void testWithOneSni() throws Exception {
		
		final String server = "jpop.mail.yahoo.com";
		final int port = 993;

		final Bootstrap bs = new Bootstrap();
		final NioEventLoopGroup group = new NioEventLoopGroup(2);

		// final SslContext sslContext = SslContextBuilder.forClient().build();
		bs.group(group);
		bs.channel(NioSocketChannel.class);
		bs.option(ChannelOption.SO_KEEPALIVE, true);
		
		
        //bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        bs.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                final List<SNIServerName> sniList = new ArrayList<SNIServerName>();
                sniList.add(new SNIHostName(server));
                final SSLParameters params = new SSLParameters();
                params.setServerNames(sniList);
                SSLEngine engine = SslContextBuilder.forClient().build().newEngine(ch.alloc());
                engine.setSSLParameters(params);
                
				p.addLast("sslHandler", new SslHandler(engine));
            }

        });
        
        System.out.println("connecting to 4443");
            ChannelFuture future = bs.connect("127.0.0.1", 4443).sync();
            if (future.isDone()) {
            	System.out.println("done");
            }
            
            Thread.sleep(5000);
	}

}
