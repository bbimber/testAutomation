/*
 * Copyright (c) 2008-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.dumbster.model;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.apache.log4j.Logger;
import org.labkey.api.util.ContextListener;
import org.labkey.api.util.MailHelper;
import org.labkey.api.util.ShutdownListener;

import javax.mail.Session;
import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * <code>DumbsterManager</code>
 */
public class DumbsterManager implements ShutdownListener
{
    private static final Logger _log = Logger.getLogger(DumbsterManager.class);

    private static DumbsterManager instance;

    public static DumbsterManager get()
    {
        return instance;
    }

    public static void setInstance(DumbsterManager instance)
    {
        DumbsterManager.instance = instance;
    }

    SimpleSmtpServer _server;

    public boolean start()
    {
        int port;
        ServerSocket socket = null;
        try
        {
            socket = new ServerSocket(0);
            port = socket.getLocalPort();
        }
        catch (IOException e)
        {
            _log.error("Failed to open a server socket", e);
            return false;
        }
        finally
        {
            try
            {
                if (socket != null)
                    socket.close();
            }
            catch (IOException e) {}
        }
        
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.user", "Anonymous");
        props.setProperty("mail.smtp.port", Integer.toString(port));
        Session session = Session.getInstance(props);

        _log.info("Switching MailHelper to use port " + port);
        MailHelper.setSession(session);

        _log.info("Connecting mail recorder to port " + port);        
        _server = SimpleSmtpServer.start(port);
        if (_server.isStopped())
        {
            _log.error("Failed to connect mail recorder. Port " + port + " may be in use.");
            _server = null;
            return false;
        }
        ContextListener.addShutdownListener(this);
        return true;
    }

    public void stop()
    {
        // Stop the server, if there is one, but leave it around for
        // viewing until the next call to start() overwrites.
        if (_server != null)
        {
            _log.info("Reverting MailHelper to labkey.xml configuration");
            MailHelper.setSession(null);

            _server.stop();
            ContextListener.removeShutdownListener(this);
        }
    }
    
    public void shutdownPre(ServletContextEvent servletContextEvent)
    {
    }

    public void shutdownStarted(ServletContextEvent servletContextEvent)
    {
        // Stop listening on the mail port before shutdown.
        stop();
    }

    public boolean isRecording()
    {
        return _server != null && !_server.isStopped();
    }

    public SmtpMessage[] getMessages()
    {
        if (_server == null)
            return new SmtpMessage[0];

        List<SmtpMessage> messageList = new ArrayList<SmtpMessage>();

        // Dumbster returns iterator on list which requires synchronization.
        synchronized (_server)
        {
            Iterator it = _server.getReceivedEmail();
            while (it.hasNext())
            {
                messageList.add((SmtpMessage) it.next());
            }
        }

        // Reverse the list to put most recent at the top
        SmtpMessage[] messages = new SmtpMessage[messageList.size()];
        for (int i = 0; i < messages.length; i++)
            messages[i] = messageList.get(messages.length - i - 1);

        return messages;
    }
}
