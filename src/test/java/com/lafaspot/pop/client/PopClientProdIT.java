/**
 *
 */
package com.lafaspot.pop.client;

import java.util.concurrent.ExecutionException;

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

/**
 * @author kraman
 *
 */
public class PopClientProdIT {


    // private final String server = "jpop200006x.mail.ne1.yahoo.com";
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
    public void testCommands() throws PopException, InterruptedException, ExecutionException {

        PopSession sess = client.createSession();
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);

        f.get();
        f = sess.execute(new PopCommand(PopCommand.Type.CAPA));
        System.out.print(f.get());

        f = sess.execute(new PopCommand(PopCommand.Type.USER).addArgs("krinteg1"));
        System.out.println(f.get());

		f = sess.execute(new PopCommand(PopCommand.Type.PASS).addArgs("**"));
        System.out.println(f.get());

        f = sess.execute(new PopCommand(PopCommand.Type.UIDL));
        System.out.println(f.get());

        f = sess.execute(new PopCommand(PopCommand.Type.LIST));
        System.out.println(f.get());

        f = sess.execute(new PopCommand(PopCommand.Type.CAPA));
        System.out.println(f.get());
    }

    @Test
    public void testCommandsPipeline1() throws PopException, InterruptedException, ExecutionException {

        PopSession sess = client.createSession();
        PopCommand cmd;
        PopFuture<PopCommandResponse> f = sess.connect(server, port, 120000, 120000);
        f.get();

        cmd = new PopCommand(PopCommand.Type.CAPA);
        System.out.println("firing " + cmd);
        f = sess.execute(cmd);
        System.out.print(f.get());
        cmd = new PopCommand(PopCommand.Type.USER).addArgs("krinteg1");
        f = sess.execute(cmd);
        System.out.println(f.get());
		cmd = new PopCommand(PopCommand.Type.PASS).addArgs("**");
        f = sess.execute(cmd);
        System.out.println(f.get());

        PopCommand cmd1 = new PopCommand(PopCommand.Type.UIDL);
        PopCommand cmd2 = new PopCommand(PopCommand.Type.LIST);
        PopCommand cmd3 = new PopCommand(PopCommand.Type.CAPA);
        System.out.println("firing " + cmd1);
        PopFuture<PopCommandResponse> f1 = sess.execute(cmd1);
        System.out.println("firing " + cmd2);
        PopFuture<PopCommandResponse> f2 = sess.execute(cmd2);
        System.out.println("firing " + cmd3);
        PopFuture<PopCommandResponse> f3 = sess.execute(cmd3);
        System.out.println(f1.get());
        System.out.println(f2.get());
        System.out.println(f3.get());
    }




}
