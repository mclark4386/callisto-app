package com.qweex.callisto.chat;

import android.util.Log;
import com.qweex.callisto.R;
import com.qweex.utils.ResCache;
import com.sorcix.sirc.*;

/** Extension of sIRC that handles the IRC events, parses them, and passes them on to ChatFragment.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class IrcServiceListener implements ServerListener, MessageListener, ModeListener {
    String TAG = "Callisto:chat_tab:IrcServiceListener";

    ChatFragment chat;

    public IrcServiceListener(ChatFragment chatFragment) {
        this.chat = chatFragment;
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Server actions //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onConnect(IrcConnection irc) {
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.convo_connected),
                null,
                IrcMessage.Type.CONNECTION
        ), true);
    }

    @Override
    public void onDisconnect(IrcConnection irc) {
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.convo_disconnected),
                null,
                IrcMessage.Type.CONNECTION
        ), true);
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, String message) {
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.convo_notice, sender.getNick()),
                message,
                IrcMessage.Type.NOTICE
        ), true);
    }

    @Override
    public void onMotd(IrcConnection irc, String motd) {
        chat.receive(irc, new IrcMessage(
                null,
                motd,
                IrcMessage.Type.MOTD
        ), false);
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Private Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, String action) {
        chat.receive(irc, sender, new IrcMessage(
                sender.getNick(),
                action,
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onPrivateMessage(IrcConnection irc, User sender, String message) {
        chat.receive(irc, sender, new IrcMessage(
                sender.getNick(),
                message,
                IrcMessage.Type.MESSAGE
        ));
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, Channel target, String action) {
        chat.receive(irc, target, new IrcMessage(
                sender.getNick(),
                action,
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
        chat.receive(irc, target, new IrcMessage(
                sender.getNick(),
                message,
                IrcMessage.Type.MESSAGE
        ));
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Events //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onTopic(IrcConnection irc, Channel channel, User sender, String topic) {

        Log.d(TAG, "Topic set by " + sender);
        String msg = ResCache.str(R.string.convo_topic, topic, sender==null ? "unknown" : sender.getNick());

        IrcMessage imsg = new IrcMessage(
                null,
                "*** " + msg,
                IrcMessage.Type.ACTION
        );
        chat.receive(irc, channel, imsg);
    }

    @Override
    public void onJoin(IrcConnection irc, Channel channel, User user) {
        String msg;
        if(user.isUs())
            msg = " *** " + ResCache.str(R.string.convo_join_me);
        else
            msg = " *** " + ResCache.str(R.string.convo_join, user.getNick());
        chat.receive(irc, channel, new IrcMessage(
                null,
                msg,
                IrcMessage.Type.JOIN
        ));
    }

    @Override
    public void onKick(IrcConnection irc, Channel channel, User sender, User user, String message) {
        String msg;
        if(sender.isUs())
            msg = ResCache.str(R.string.convo_kick_me_active, user.getNick(), message);
        else if(user.isUs())
            msg = ResCache.str(R.string.convo_kick_me_passive, sender.getNick(), message);
        else
            msg = ResCache.str(R.string.convo_kick, sender.getNick(), user.getNick(), message);

        chat.receive(irc, channel, new IrcMessage(
                null,
                " *** " + msg,
                IrcMessage.Type.KICK
        ));
    }

    @Override
    public void onPart(IrcConnection irc, Channel channel, User user, String message) {
        String msg;
        if(user.isUs())
            msg = ResCache.str(R.string.convo_part_me, message);
        else
            msg = ResCache.str(R.string.convo_part, user.getNick(),message);

        chat.receive(irc, channel, new IrcMessage(
                null,
                " *** " + msg,
                IrcMessage.Type.PART
        ));
    }

    // More adequately onChannelMode
    @Override
    public void onMode(IrcConnection irc, Channel channel, User sender, String mode) {
        String msg;
        if(sender.isUs())
            msg = ResCache.str(R.string.convo_mode_channel_me, sender.getNick(), mode);
        else
            msg = ResCache.str(R.string.convo_mode_channel, sender.getNick(), mode);

        chat.receive(irc, channel, new IrcMessage(
                null,
                " *** " + msg,
                IrcMessage.Type.KICK
        ));
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, Channel target, String message) {
        chat.receive(irc, target, new IrcMessage(
                ResCache.str(R.string.convo_notice, sender.getNick()),
                message,
                IrcMessage.Type.NOTICE
        ));
    }


    ///////////////////////////////////////////////////////////////////////////////
    //////////////// User Mode Changes (subtype of Channel Events) ////////////////
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public void onFounder(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.FOUNDER, true);
    }

    @Override
    public void onDeFounder(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.FOUNDER, false);
    }

    @Override
    public void onAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.ADMIN, true);
    }

    @Override
    public void onDeAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.ADMIN, false);
    }

    @Override
    public void onOp(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.OP, true);
    }

    @Override
    public void onDeOp(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.OP, false);
    }

    @Override
    public void onHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.HALFOP, true);
    }

    @Override
    public void onDeHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.HALFOP, false);
    }

    @Override
    public void onVoice(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.VOICE, true);
    }

    @Override
    public void onDeVoice(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(irc, channel, sender, user, IrcMessage.UserMode.VOICE, false);
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Propogatable Events //////////////////////////
    /////////////////////////////////////////////////////////////////////////

    @Override
    public void onNick(IrcConnection irc, User oldUser, User newUser) {
        String msg;
        if(newUser.isUs())
            msg = ResCache.str(R.string.convo_nick_me, newUser.getNick());
        else
            msg = ResCache.str(R.string.convo_nick, oldUser.getNick(), newUser.getNick());

        chat.receive(irc, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.NICK,
                oldUser
        ), true);
    }

    @Override
    public void onQuit(IrcConnection irc, User user, String message) {
        String msg;
        if(user.isUs())
            msg = ResCache.str(R.string.convo_quit_me, message);
        else
            msg = ResCache.str(R.string.convo_quit, user.getNick(), message);

        chat.receive(irc, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.QUIT,
                user
        ), true);
    }

    //////////////////////////////////////////////////////////
    ////////////////////////// Misc //////////////////////////
    //////////////////////////////////////////////////////////

    @Override
    public void onCtcpReply(IrcConnection irc, User sender, String command, String message) {
        //TODO
        
    }


    @Override
    public void onInvite(IrcConnection irc, User sender, User user, Channel channel) {
        //TODO
        
    }


    //////////////////////////////////////////////////////////////
    ////////////////////////// Non-sIRC //////////////////////////
    //////////////////////////////////////////////////////////////


    // Mode Change
    public void handleModeChange(IrcConnection irc, Channel channel, User setBy, User setTarget, IrcMessage.UserMode mode, boolean given) {
        String msg;
        String modeString = mode.toString();
        modeString = modeString.charAt(0) + modeString.substring(1).toLowerCase();
        if(given) {
            if(setBy.isUs())
                msg = ResCache.str(R.string.convo_mode_set_me_active, modeString, setTarget.getNick());
            else if(setTarget.isUs())
                msg = ResCache.str(R.string.convo_mode_set_me_passive, setBy.getNick(), modeString);
            else
                msg = ResCache.str(R.string.convo_mode_set, setBy.getNick(), modeString, setTarget.getNick());
        } else {
            if(setBy.isUs())
                msg = ResCache.str(R.string.convo_mode_unset_me_active, modeString, setTarget.getNick());
            else if(setTarget.isUs())
                msg = ResCache.str(R.string.convo_mode_unset_me_passive, setBy.getNick(), modeString);
            else
                msg = ResCache.str(R.string.convo_mode_unset, setBy.getNick(), modeString, setTarget.getNick());
        }

        Log.d(TAG, msg);

        chat.receive(irc, channel, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.MODE
        ));
    }
}
