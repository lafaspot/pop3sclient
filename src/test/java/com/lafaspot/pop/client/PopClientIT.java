/**
 *
 */
package com.lafaspot.pop.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;
import com.lafaspot.pop.command.PopCommand;
import com.lafaspot.pop.command.PopCommand.Type;
import com.lafaspot.pop.command.PopCommandResponse;
import com.lafaspot.pop.exception.PopException;
import com.lafaspot.pop.session.PopSession;

import io.netty.channel.ChannelOption;

/**
 * @author kraman
 *
 */
public class PopClientIT {

    private PopClient client;
    private LogManager logManager;
    private Logger logger;
//    private final String server = "localhost"; // "jpop.pop.mail.yahoo.com";
//    private final int port = 9995;
    private final String server = "jpop.pop.mail.yahoo.com";
    private final int port = 995;



    @BeforeClass
    public void beforeClass() throws PopException {

        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        logger = logManager.getLogger(new LogContext(PopClientIT.class.getName()) {
        });
        client = new PopClient(10, logManager);
    }

    @Test
    public void testConnectWithProperties() throws PopException, InterruptedException, ExecutionException {
        final Properties p = new Properties();
        p.put(ChannelOption.AUTO_READ.name(), true);
        final PopClient c = new PopClient(10, logManager, p);
        PopSession session = c.createSession();
        Future<PopCommandResponse> f = session.connect(server, port, 30000, 60000);

        f.get();
        System.out.println("connect is complete, sending USER command");
        final PopCommand user = new PopCommand(PopCommand.Type.USER);
        user.addArgs("krinteg2@yahoo.com");
        Future<PopCommandResponse> f2 = session.execute(user);
        session.disconnect();
        System.out.println("testConnectWithProperties DONE");
    }

    /**
     * Run Server as:
     * openssl s_server -key key.pem -cert cert.pem -accept 9995 -tlsextdebug
     * @throws PopException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test(enabled = true, expectedExceptions = ExecutionException.class, expectedExceptionsMessageRegExp = ".*CHANNEL_DISCONNECTED.*")
    public void testConnectWithoutSni() throws PopException, InterruptedException, ExecutionException {
        PopSession session = client.createSession();
        int c;
        PopCommandResponse r;

        Future<PopCommandResponse> f = session.connect("127.0.0.1", 9995, 30000, 60000);
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "connect command failed");


        f = session.execute(new PopCommand(PopCommand.Type.CAPA));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "capa command not done");
        r = f.get();

        Assert.fail("Should not come here");
    }


    /**
     * Run server as :
     * openssl s_server -key key.pem -cert cert.pem -accept 9995 -tlsextdebug
     * @throws PopException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test(enabled = true, expectedExceptions = ExecutionException.class, expectedExceptionsMessageRegExp = ".*CHANNEL_DISCONNECTED.*")
    public void testConnectWithSni() throws PopException, InterruptedException, ExecutionException {
        PopSession session = client.createSession();
        List<String> sni = new ArrayList<String>();
        sni.add("test1.mail.aol.com");

        int c;
        PopCommandResponse r;
        Future<PopCommandResponse> f = session.connect("127.0.0.1", 9995, 30000, 60000, sni);
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "connect command failed");

        f = session.execute(new PopCommand(PopCommand.Type.CAPA));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
        Assert.fail("Should not have come here");
    }


    /**
     * Run server as :
     * openssl s_server -key key.pem -cert cert.pem -accept 9995 -tlsextdebug
     * @throws PopException
     * @throws InterruptedException
     * @throws ExecutionException
     */

    @Test(enabled = true, expectedExceptions = PopException.class, expectedExceptionsMessageRegExp = ".*INVALID_ARGUMENTS.*")
    public void testConnectWithInvalidSni() throws PopException, InterruptedException, ExecutionException {

        final List<String> sni = new ArrayList<String>();
        sni.add("a.b.");
        PopSession session = client.createSession();

        Future<PopCommandResponse> f;
        int c;
        PopCommandResponse r;

        f = session.connect("127.0.0.1", 9995, 30000, 60000, sni); c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "connect command failed");

        f = session.execute(new PopCommand(PopCommand.Type.CAPA));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "capa command not done");
        r = f.get();
        Assert.fail("Should not have come here.");

    }

    @Test
    public void testConnect() throws PopException, InterruptedException, ExecutionException {

        PopSession session = client.createSession();
        Future<PopCommandResponse> f = session.connect(server, port, 30000, 60000);
        System.out.println("connect sent, waiting on get");

        int i=0;
        while (i++ < 10) {
            if (f.isDone()) {
                System.out.println("connect sent and done, waiting on get");
                PopCommandResponse r = f.get();
                System.out.println("Got connect response " + r.getLines());
                break;
            }
            Thread.sleep(100);
        }


        System.out.println("conntect is complete, sending capa");
        Future<PopCommandResponse> f2 = session.execute(new PopCommand(PopCommand.Type.CAPA));
        System.out.println("sent capa, waiting for resp. " + f2);
        f2.get();

        System.out.println("capa " + f2.get().getLines());

        System.out.println("testConnect DONE ");
       Future <PopCommandResponse> f3 = session.execute(new PopCommand(PopCommand.Type.QUIT));
       f3.get();
    }

    @Test
    public void testUidl() throws PopException, InterruptedException, ExecutionException {
        System.out.println("Testing testUidlListRetr");
        final PopSession session = client.createSession();

        int c = 0;
        PopCommandResponse r;
        Future<PopCommandResponse> f = session.connect(server, port, 30000, 90000);

        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "connect command failed");


        PopCommand user = new PopCommand(Type.USER);
        user.addArgs("krinteg2@yahoo.com");
        f = session.execute(user);

        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "user command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "user command failed");

        PopCommand pass = new PopCommand(Type.PASS);
        pass.addArgs("**");
        f = session.execute(pass);
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "pass command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "pass command failed");



        f = session.execute(new PopCommand(Type.UIDL));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
       // Assert.assertTrue(f.isDone(), "UID command not done");
         r = f.get();
        Assert.assertTrue(r.isOk(), "UID command failed");


        f = session.execute(new PopCommand(Type.QUIT));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "quit command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "quit command failed");
    }

    @Test(enabled = true)
    public void testUidlListRetr() throws PopException, InterruptedException, ExecutionException {
        System.out.println("Testing testUidlListRetr");
        final PopSession session = client.createSession();

        int c = 0;
        PopCommandResponse r;
        Future<PopCommandResponse> f = session.connect(server, port, 30000, 90000);

        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "connect command failed");


        PopCommand user = new PopCommand(Type.USER);
        user.addArgs("krinteg2@yahoo.com");
        f = session.execute(user);

        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "user command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "user command failed");

        PopCommand pass = new PopCommand(Type.PASS);
        pass.addArgs("**");
        f = session.execute(pass);
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "pass command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "pass command failed");

        f = session.execute(new PopCommand(Type.UIDL));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        //Assert.assertTrue(f.isDone(), "pass command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "pass command failed");
        Assert.assertTrue(f.get().getLines().size() > 0);

        f = session.execute(new PopCommand(Type.LIST));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        //Assert.assertTrue(f.isDone(), "list command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "list command failed");
        Assert.assertTrue(f.get().getLines().size() > 0);

        PopCommand retrCmd = new PopCommand(Type.RETR);
        retrCmd.addArgs("2");
        f = session.execute(retrCmd);
        c=0;
        while (c++ < 60) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(1000);
        }


        //Assert.assertTrue(f.isDone(), "retr command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "retr command failed " + r.getLines());

        Assert.assertTrue(f.get().getLines().size() > 0);
        System.out.println(f.get().getLines());


        f = session.execute(new PopCommand(Type.QUIT));
        c=0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "quit command not done");
        r = f.get();
       Assert.assertTrue(r.isOk(), "quit command failed");
    }

    @Test (expectedExceptions = PopException.class)
    public void testInactivity() throws PopException, InterruptedException, ExecutionException {

        int c;
        PopCommandResponse r;
        // String server = "localhost";
        // int port = 9995;
        PopSession session = client.createSession();
        Future<PopCommandResponse> f = session.connect(server, port, 1000, 1000);
        c = 0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "connect command not done");
        r = f.get();
        Assert.assertTrue(r.isOk(), "connect command failed");

        f = session.execute(new PopCommand(PopCommand.Type.CAPA));
        c = 0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "capa command not done");
        r = f.get();
        Assert.assertTrue(r.isOk(), "capa command failed");

        System.out.println("sleeping for 1s");
        Thread.sleep(1000);
        System.out.println("good morning, awake");

        f = session.execute(new PopCommand(PopCommand.Type.QUIT));
        c = 0;
        while (c++ < 10) {
            if (f.isDone()) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue(f.isDone(), "quit command not done");
        r = f.get();
        Assert.assertTrue(r.isOk(), "quit command failed");

    }

}
