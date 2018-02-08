package com.lafaspot.pop.command;

import java.util.ArrayList;
import java.util.Arrays;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.pop.session.PopFuture;

import io.netty.channel.ChannelFuture;

/**
 * Unit test for {@link PopCommand}.
 * 
 * @author fsu
 *
 */
public class PopCommandTest {

    /**
     * Tests for constructor.
     */
    @Test
    public void testConstructor() {
        final PopCommand popCommand = new PopCommand(PopCommand.Type.USER);
        Assert.assertEquals(popCommand.getCommandLine(), "USER\n", "Command line should match");
        Assert.assertEquals(popCommand.toString(), "USER\n", "toString should match");
        Assert.assertEquals(popCommand.getType(), PopCommand.Type.USER, "Command type should match");
        Assert.assertNull(popCommand.getCommandFuture(), "Command future should be null");
        Assert.assertNotNull(popCommand.getResponse(), "Response should be not null");
        Assert.assertEquals(popCommand.getResponse().getCommand(), popCommand, "Command of response should be not null");
        Assert.assertEquals(popCommand.getResponse().getLines(), new ArrayList<String>(), "Lines of response should be not null");
        
        final PopCommand popCommand2 = new PopCommand("USER", PopCommand.Type.USER, Arrays.asList("email"));
        Assert.assertEquals(popCommand2.getCommandLine(), "USER email\n", "Command line should match");
        Assert.assertEquals(popCommand2.toString(), "USER email\n", "toString should match");
        Assert.assertEquals(popCommand2.getType(), PopCommand.Type.USER, "Command type should match");
        Assert.assertNull(popCommand2.getCommandFuture(), "Command future should be null");
        Assert.assertNotNull(popCommand2.getResponse(), "Response should be not null");
        Assert.assertEquals(popCommand2.getResponse().getCommand(), popCommand2, "Command of response should be not null");
        Assert.assertEquals(popCommand2.getResponse().getLines(), new ArrayList<String>(), "Lines of response should be not null");
    }
    
    /**
     * Tests for set command future.
     */
    @Test
    public void testSetCommandFuture() {
        final PopCommand popCommand = new PopCommand(PopCommand.Type.USER);
        final ChannelFuture future = Mockito.mock(ChannelFuture.class);
        final PopFuture<PopCommandResponse> commandFuture = new PopFuture<PopCommandResponse>(future);
        popCommand.setCommandFuture(commandFuture);
        Assert.assertEquals(popCommand.getCommandFuture(), commandFuture, "Command future should match");
    }
    
    /**
     * Tests for add arguments.
     */
    @Test
    public void testAddArgs() {
        final PopCommand popCommand = new PopCommand(PopCommand.Type.USER).addArgs("email");
        Assert.assertEquals(popCommand.getCommandLine(), "USER email\n", "Command line should match");
        Assert.assertEquals(popCommand.toString(), "USER email\n", "toString should match");
        Assert.assertEquals(popCommand.getType(), PopCommand.Type.USER, "Command type should match");
        Assert.assertNull(popCommand.getCommandFuture(), "Command future should be null");
        Assert.assertNotNull(popCommand.getResponse(), "Response should be not null");
        Assert.assertEquals(popCommand.getResponse().getCommand(), popCommand, "Command of response should be not null");
        Assert.assertEquals(popCommand.getResponse().getLines(), new ArrayList<String>(), "Lines of response should be not null");
    }
    
    /**
     * Tests for length of command types.
     */
    @Test
    public void testCommandTypesLength() {
        Assert.assertEquals(PopCommand.Type.values().length, 17, "Pop command type length should match");
    }
    
    /**
     * Tests for multiLine of command type.
     */
    @Test
    public void testCommandTypeMultiLine() {
        Assert.assertFalse(PopCommand.Type.INVALID_POP_COMMAND_CONNECT.multiLine(), "INVALID command multiLine should match");
        Assert.assertFalse(PopCommand.Type.USER.multiLine(), "USER command multiLine should match");
        Assert.assertFalse(PopCommand.Type.PASS.multiLine(), "PASS command multiLine should match");
        Assert.assertFalse(PopCommand.Type.STAT.multiLine(), "STAT command multiLine should match");
        Assert.assertTrue(PopCommand.Type.LIST.multiLine(), "LIST command multiLine should match");
        Assert.assertTrue(PopCommand.Type.RETR.multiLine(), "RETR command multiLine should match");
        Assert.assertFalse(PopCommand.Type.DELE.multiLine(), "DELE command multiLine should match");
        Assert.assertFalse(PopCommand.Type.NOOP.multiLine(), "NOOP command multiLine should match");
        Assert.assertFalse(PopCommand.Type.RSET.multiLine(), "RSET command multiLine should match");
        Assert.assertTrue(PopCommand.Type.TOP.multiLine(), "RSET command multiLine should match");
        Assert.assertTrue(PopCommand.Type.UIDL.multiLine(), "UIDL command multiLine should match");
        Assert.assertFalse(PopCommand.Type.QUIT.multiLine(), "QUIT command multiLine should match");
        Assert.assertTrue(PopCommand.Type.CAPA.multiLine(), "CAPA command multiLine should match");
        Assert.assertFalse(PopCommand.Type.AUTH.multiLine(), "AUTH command multiLine should match");
        Assert.assertFalse(PopCommand.Type.LAST.multiLine(), "LAST command multiLine should match");
        Assert.assertFalse(PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE.multiLine(), "GENERIC_STRING_COMMAND_SINGLELINE command multiLine should match");
        Assert.assertTrue(PopCommand.Type.GENERIC_STRING_COMMAND_MULTILINE.multiLine(), "GENERIC_STRING_COMMAND_MULTILINE command multiLine should match");
    }
    
    /**
     * Test valueFromString.
     */
    @Test
    public void testValueFromString() {
        Assert.assertEquals(PopCommand.Type.valueFromString("CAPA"), PopCommand.Type.CAPA, "Command type should match");
        Assert.assertEquals(PopCommand.Type.valueFromString("capa"), PopCommand.Type.CAPA, "Command type should match");
        Assert.assertEquals(PopCommand.Type.valueFromString("CommandString"), PopCommand.Type.GENERIC_STRING_COMMAND_SINGLELINE, "Command type should match");
    }
}
