/**
 *
 */
package com.lafaspot.pop.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;
import com.lafaspot.pop.command.PopCommand;
import com.lafaspot.pop.command.PopCommandResponse;
import com.lafaspot.pop.exception.PopException;
import com.lafaspot.pop.session.PopFuture;
import com.lafaspot.pop.session.PopSession;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * @author kraman
 *
 */
public class PopClientProdIT {

    private final String server = "jpop200002.pop.mail.yahoo.com";
    private final int port = 995;
    private PopClient client;

    @BeforeClass
    public void init() throws PopException {
        final LogManager logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        final Logger logger = logManager.getLogger(new LogContext(PopClientProdIT.class.getName()) {
        });
        client = new PopClient(10, logManager);
    }

    @Test
    public void testChannelPipeline() throws PopException, InterruptedException, ExecutionException {
        PopSession sess = client.createSession();
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);
        f.get();
        final Channel ch = sess.getChannel();
        Assert.assertNotNull(ch);
        final ChannelPipeline pipe = ch.pipeline();
        Assert.assertNotNull(pipe);
        final String[] handlerList = { PopSession.SSL_HANDLER, PopSession.INACTIVITY_HANDLER, PopSession.DELIMITER, PopSession.DECODER,
                PopSession.ENCODER, PopSession.POP_HANDLER };
        final Iterator<Entry<String, ChannelHandler>> it = pipe.iterator();
        int index = 0;
        while (it.hasNext()) {
            final Entry<String, ChannelHandler> entry = it.next();
            Assert.assertNotNull(entry);
            final String name = entry.getKey();
            Assert.assertEquals(name, handlerList[index++]);
            Assert.assertNotNull(entry.getValue());
        }
        sess.disconnect();
    }

    @Test
    public void testChangeChannelPipeline() throws PopException, InterruptedException, ExecutionException {
        PopSession sess = client.createSession();
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);
        f.get();
        final Channel ch = sess.getChannel();
        Assert.assertNotNull(ch);
        final ChannelPipeline pipe = ch.pipeline();
        Assert.assertNotNull(pipe);
        final String[] handlerList = { PopSession.SSL_HANDLER, PopSession.INACTIVITY_HANDLER, PopSession.DELIMITER, PopSession.DECODER,
                PopSession.ENCODER, PopSession.POP_HANDLER };
        final Iterator<Entry<String, ChannelHandler>> it = pipe.iterator();
        int index = 0;
        while (it.hasNext()) {
            final Entry<String, ChannelHandler> entry = it.next();
            Assert.assertNotNull(entry);
            final String name = entry.getKey();
            Assert.assertEquals(name, handlerList[index++]);
            Assert.assertNotNull(entry.getValue());
        }

        // Changing the pipeline by removing few handler and adding new handlers.
        final ChannelHandler newHandler = new LineBasedFrameDecoder(1);
        pipe.addAfter(PopSession.SSL_HANDLER, "NEW_HANDLER", newHandler);
        pipe.remove(PopSession.INACTIVITY_HANDLER);
        pipe.remove(PopSession.DELIMITER);
        pipe.remove(PopSession.DECODER);
        pipe.remove(PopSession.ENCODER);
        pipe.remove(PopSession.POP_HANDLER);

        final String[] newHandlerList = { PopSession.SSL_HANDLER, "NEW_HANDLER" };
        final Iterator<Entry<String, ChannelHandler>> it2 = pipe.iterator();
        int index2 = 0;
        while (it2.hasNext()) {
            final Entry<String, ChannelHandler> entry = it2.next();
            Assert.assertNotNull(entry);
            final String name = entry.getKey();
            Assert.assertEquals(name, newHandlerList[index2++]);
            Assert.assertNotNull(entry.getValue());
        }
        sess.disconnect();
    }

    @Test
    public void testDisconnectDuringChangeChannelPipeline() throws PopException, InterruptedException, ExecutionException {
        PopSession sess = client.createSession();
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);
        f.get();
        final Channel ch = sess.getChannel();
        Assert.assertNotNull(ch);
        final ChannelPipeline pipe = ch.pipeline();
        Assert.assertNotNull(pipe);
        final String[] handlerList = { PopSession.SSL_HANDLER, PopSession.INACTIVITY_HANDLER, PopSession.DELIMITER, PopSession.DECODER,
                PopSession.ENCODER, PopSession.POP_HANDLER };
        final Iterator<Entry<String, ChannelHandler>> it = pipe.iterator();
        int index = 0;
        while (it.hasNext()) {
            final Entry<String, ChannelHandler> entry = it.next();
            Assert.assertNotNull(entry);
            final String name = entry.getKey();
            Assert.assertEquals(name, handlerList[index++]);
            Assert.assertNotNull(entry.getValue());
        }

        // Changing the pipeline by removing few handlers
        pipe.remove(PopSession.INACTIVITY_HANDLER);
        pipe.remove(PopSession.DELIMITER);
        pipe.remove(PopSession.DECODER);
        pipe.remove(PopSession.ENCODER);
        pipe.remove(PopSession.POP_HANDLER);

        // Addition of new handler after disconnection doesn't affect the pipeline.
        // The individual handlers should act on channel only if it is active.
        ch.disconnect();
        final ChannelHandler newHandler = new LineBasedFrameDecoder(1);
        pipe.addLast("NEW_HANDLER", newHandler);

        final String[] newHandlerList = { PopSession.SSL_HANDLER, "NEW_HANDLER" };
        final Iterator<Entry<String, ChannelHandler>> it2 = pipe.iterator();
        int index2 = 0;
        while (it2.hasNext()) {
            final Entry<String, ChannelHandler> entry = it2.next();
            Assert.assertNotNull(entry);
            final String name = entry.getKey();
            Assert.assertEquals(name, newHandlerList[index2++]);
            Assert.assertNotNull(entry.getValue());
        }
    }

    @Test
    public void testCommands() throws PopException, InterruptedException, ExecutionException {

        PopSession sess = client.createSession();
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);

        f.get();
		f = sess.execute(
				new PopCommand("CAPA", PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE, new ArrayList<String>()));
		System.out.println(f.get());

		f = sess.execute(
				new PopCommand("USER", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>())
						.addArgs("krinteg1"));
        System.out.println(f.get());

		f = sess.execute(
				new PopCommand("PASS", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE,
						Arrays.asList(new String[] { "*" })));
        System.out.println(f.get());

		f = sess.execute(
				new PopCommand("UIDL", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE,
						Arrays.asList(new String[] { "1" })));
        System.out.println(f.get());

		f = sess.execute(
				new PopCommand("LIST", PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE, new ArrayList<String>()));
        System.out.println(f.get());

		f = sess.execute(
				new PopCommand("QUIT", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>()));
		System.out.println(f.get());
    }

    @Test
    public void testCommandsPipeline1() throws PopException, InterruptedException, ExecutionException {

        PopSession sess = client.createSession();
        PopCommand cmd;
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);
		// f.get();

		cmd = new PopCommand("CAPA", PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE, new ArrayList<String>());
        System.out.println("firing " + cmd);
        f = sess.execute(cmd);
		// System.out.println(f.get());
		cmd = new PopCommand("USER", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE,
				Arrays.asList(new String[] { "krinteg1" }));
		System.out.println("firing " + cmd);
        f = sess.execute(cmd);
		// System.out.println(f.get());
		cmd = new PopCommand("PASS", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE,
				Arrays.asList(new String[] { "*" }));
		System.out.println("firing " + cmd);
        f = sess.execute(cmd);
		// System.out.println(f.get());

		PopCommand cmd0 = new PopCommand("UIDL", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE,
				Arrays.asList(new String[] { "2" }));
		PopCommand cmd1 = new PopCommand("UIDL", PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE,
				new ArrayList<String>());
		PopCommand cmd2 = new PopCommand("LIST", PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE,
				new ArrayList<String>());
		PopCommand cmd3 = new PopCommand("CAPA", PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE,
				new ArrayList<String>());
		System.out.println("firing " + cmd0);
		PopFuture<PopCommandResponse> f0 = sess.execute(cmd0);
        System.out.println("firing " + cmd1);
        PopFuture<PopCommandResponse> f1 = sess.execute(cmd1);
        System.out.println("firing " + cmd2);
        PopFuture<PopCommandResponse> f2 = sess.execute(cmd2);
        System.out.println("firing " + cmd3);
        PopFuture<PopCommandResponse> f3 = sess.execute(cmd3);
        System.out.println(f1.get());
        System.out.println(f2.get());
        System.out.println(f3.get());

		f = sess.execute(
				new PopCommand("QUIT", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>()));
		System.out.println(f.get());
    }

	@Test
	public void testCommandsPipeline2() throws PopException, InterruptedException, ExecutionException {

	    PopSession sess = client.createSession();
	    PopCommand cmd;
	    PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);
		System.out.println("waiting for connect resp " + f.get());

		String oauthToken = "vWO7LpiauF0qAjaTfGmzcZnxyu3k9tleWQ.n5dfO.lAeNC7XX0OzHqJzou77HKLq6Ft6jWvIrpeAfXE4H26f7tUuHKLBdlRJ_I69cjQqzvgoCVlYax8Q8F.35P7VCutBe2p6fUfmR6bnGZYhnWpKm40.aUxaA74E_zm_uwFRApou.CEUoIc.LKcWncpDfVmkN9GouvpuokXnIXq_KUxrDzKLn9G9U8KDZMa6bjPxE8tFqjkPEyN18xP_Uh7x1akh13Wgh1C4jvpMiZFMW8X9w3wxDCveXk8qPHrccr83tHWxVAgSAOJEWNArzpK2k0mH0lUZr_rhZcb1e9wbUsANosxapni4DQY3l6gWXvg2RHo5UaurnwXWjgWo764Gqd8FibKRmUxIMAfOzoU19MWGPosddz9iL8Z1waeHeqLhzxqKkEnVAGwQ5RdEHOoksTbLnPWnVUuigf4FbZ6V9a4OUucT8aTQkj90g3yPxusECXCpYbAkNu.L8I6bRbs3mqTHtJMA7nFZG4A110x9xu2gt0f.SiK3BLBNvA0MmyuYfNQq58x3pfAtapCu0skrj0irXIQLKNl9HkgstZYYAddlIfUPmOXX03RsDdaYhmbgYk.KnbpmXF7NHfgjkJaxvc7HnK5qOglKqr.Ceh8UegtPqUa8_KS.IfkjwJJzCaWTCIL4LMsyDft9YXea_WPdfEspW5lgWv5CEUFYatWcVsLgBthdo4SIzmRhe8K8o1XUprvq95Yoz.3G8LFB6.SOrdOmBwqUKXql6u0cTLpGdZM6J1jhDxgZ5pjlMMLiEXQoNMzoOz3JlJm8WMKni2pzn7v9h_1oqkFvLC0POiEkqmnW6UFxjckUxlpnkBPjOI2HLG4zdoO7p5sjFHKhqDs4GjmFFHIXJMmve55vqC6ozewOBDSFMA--";
		final String combinedOauthString = "user=krinteg1@yahoo.com" + "\u0001" + "Auth=Bearer " + oauthToken
				+ "\u0001" + "\u0001";
		final byte[] encodedBytes = Base64.getEncoder().encode(combinedOauthString.getBytes(StandardCharsets.UTF_8));
		String xoauthArgument = new String(encodedBytes, StandardCharsets.UTF_8);
		cmd = new PopCommand(PopCommand.Type.AUTH).addArgs("XOAUTH2").addArgs(xoauthArgument);
		System.out.println(">>> " + cmd);
	    f = sess.execute(cmd);
		PopCommand cmd0 = new PopCommand(PopCommand.Type.STAT);
	    PopCommand cmd1 = new PopCommand(PopCommand.Type.UIDL);
	    PopCommand cmd2 = new PopCommand(PopCommand.Type.LIST);
		PopCommand cmd3 = new PopCommand(PopCommand.Type.QUIT);
		System.out.println(">>> " + cmd0);
		PopFuture<PopCommandResponse> f0 = sess.execute(cmd0);
		System.out.println(">>> " + cmd1);
	    PopFuture<PopCommandResponse> f1 = sess.execute(cmd1);
		System.out.println(">>> " + cmd2);
	    PopFuture<PopCommandResponse> f2 = sess.execute(cmd2);
		System.out.println(">>> " + cmd3);
		PopFuture<PopCommandResponse> f3 = sess.execute(cmd3);
		System.out.println(f.get());
		System.out.println(f0.get());
		System.out.println(f1.get());
	    System.out.println(f2.get());
		System.out.println(f3.get());

		sess.execute(new PopCommand(PopCommand.Type.QUIT));
		sess.disconnect();
	}

	@Test
	public void testMultiAuthCommand() throws PopException, InterruptedException, ExecutionException {

	    PopSession sess = client.createSession();
	    PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);

	    f.get();
		f = sess.execute(new PopCommand("auth", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE,
				Arrays.asList(new String[] { "login" })));
		System.out.println(f.get());

		f = sess.execute(
				new PopCommand("*", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>()));
	    System.out.println(f.get());

		f = sess.execute(
				new PopCommand("auth", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>())
						.addArgs("plain"));
	    System.out.println(f.get());

		f = sess.execute(
				new PopCommand("a3JzYWxlc0BiaXptYWlsdGVzdC5jb20Aa3JzYWxlc0BiaXptYWlsdGVzdC5jb20AaGV5VGVzdGVyMQ==",
						PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>()));
	    System.out.println(f.get());

		f = sess.execute(
				new PopCommand("LIST", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>()));
	    System.out.println(f.get());

		f = sess.execute(
				new PopCommand("QUIT", PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, new ArrayList<String>()));
		System.out.println(f.get());
	}




}
