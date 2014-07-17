package com.qweex.callisto.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.qweex.utils.ResCache;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

import java.util.HashMap;
import java.util.regex.Pattern;

/** Houses & manages the TabFragments.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class ChatFragment extends CallistoFragment {

    String TAG = "Callisto:chat:ChatFragment";

    /** Adapter that manages receiving the messages directly from the sirc library. */
    IrcServiceListener ircConnectionAdapter;

    /** Views. VIEWS! */
    RelativeLayout layout;
    ListView listview;

    /** Lists of fragments */
    HashMap<IrcConnection, ServerTabFragment> serverTabs = new HashMap<IrcConnection, ServerTabFragment>();
    HashMap<Channel, ChannelTabFragment> channelTabs = new HashMap<Channel, ChannelTabFragment>();
    HashMap<User, UserTabFragment> userTabs = new HashMap<User, UserTabFragment>();

    /** Mention Parsers */
    HashMap<IrcConnection, Pattern> mentionParsers = new HashMap<IrcConnection, Pattern>();

    /** Constructor; supplies MasterActivity reference. */
    public ChatFragment(MasterActivity master) {
        super(master);
        ircConnectionAdapter = new IrcServiceListener(this);
    }

    /** Inherited method; called each time the fragment is attached to a FragmentActivity.
     * @param inflater Used for instantiating the fragment's view.
     * @param container [ASK_SOMEONE_SMARTER]
     * @param savedInstanceState [ASK_SOMEONE_SMARTER]
     * @return The new / recycled View to be attached.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if(layout==null) {
            layout = (RelativeLayout) inflater.inflate(R.layout.chat_tab, null);

            listview = (ListView) layout.findViewById(android.R.id.list);
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    /** Create the service that will house the connection.
     * @param nickname Duh.
     * @param username Duh.
     * @param real_name Duh.
     * @param password Duh.
     * @param channels DUUUUUUUUH.
     */
    public void connect(String nickname, String username, String real_name, String password, String[] channels) {
        IrcService.chatFragment = this;

        Log.v(TAG, "Starting service");
        Intent i = new Intent(master, IrcService.class);
        i.putExtra("nickname", nickname);
        i.putExtra("username", username);
        i.putExtra("real_name", real_name);
        i.putExtra("password", password);
        i.putExtra("channels", channels);
        master.startService(i);
        Log.v(TAG, "Started service");
    }

    @Override
    public void show() {
        //TODO
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @Override
    public void hide() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////// Create a tab ////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    /** Create a server tab.
     * @param server The server to associate with the tab.
     */
    public void createTab(final IrcConnection server) {
        Log.v(TAG, "Creating Server tab");
        final ServerTabFragment tabFragment = new ServerTabFragment(master, server);
        serverTabs.put(server, tabFragment);
        updateMentionPattern(server);

        tabFragment.receive(new IrcMessage(
                ResCache.str(R.string.convo_connecting, server.getServerAddress()),
                null,
                IrcMessage.Type.CONNECTION
        ));

        _createTab(tabFragment, server.getServerAddress(), server);
    }

    /** Create a channel tab.
     * @param channel The channel to associate with the tab.
     */
    public void createTab(IrcConnection server, final Channel channel) {
        Log.v(TAG, "Creating Channel tab");
        final ChannelTabFragment tabFragment = new ChannelTabFragment(master, server, channel);
        channelTabs.put(channel, tabFragment);

        _createTab(tabFragment, server.getServerAddress() + ":" + channel.getName(), channel);
    }

    /** Create a user (private message) tab.
     * @param user The user object to associate with the tab.
     */
    public void createTab(IrcConnection server, final User user) {
        Log.v(TAG, "Creating User tab");
        final UserTabFragment tabFragment = new UserTabFragment(master, server, user);
        userTabs.put(user, tabFragment);
        _createTab(tabFragment, server.getServerAddress() + ":" + user.getHostName(), user);
    }


    private void _createTab(final TabFragment fragment, final String tabText, final Object tabTag) {
        master.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Creating User tab on UI thread");

                ActionBar.Tab tab = master.getSupportActionBar().newTab();
                tab.setText(tabText);
                tab.setTag(tabTag);
                tab.setTabListener(new ChatTabListener(fragment));
                master.getSupportActionBar().addTab(tab);

                // Check for any waiting messages that arrived before the GUI was ready
                fragment.notifyQueue();
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////// Receiving messages & passing them on to their fragments //////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    /** Receive a message destined for a Server tab
     * @param server The Server connection.
     * @param ircMessage The message.
     * @param propogate Whether or not it should be propogated to channels.
     */
    public void receive(IrcConnection server, IrcMessage ircMessage, boolean propogate) {
        if(ircMessage.propogationUser!=null || propogate) {
            for(Channel c : channelTabs.keySet())
                channelTabs.get(c).receive(ircMessage);
            for(User u : userTabs.keySet())
                userTabs.get(u).receive(ircMessage);
        } else
            serverTabs.get(server).receive(ircMessage);
    }

    /** Receive a message destined for a Channel tab
     * @param server The Server connection.
     * @param channel The target Channel connection.
     * @param ircMessage The message.
     */
    public void receive(IrcConnection server, Channel channel, IrcMessage ircMessage) {
        try {
            detectMention(server, ircMessage, server.getServerAddress() + ":" + channel.getName());
            channelTabs.get(channel).receive(ircMessage);
        } catch(NullPointerException npe) {
            Log.w(TAG, "Received message from a channel I'm not in. WTF do I do.");
            createTab(server, channel);
            channelTabs.get(channel).receive(ircMessage);
        }
    }

    /** Receive a message destined for a User tab
     * @param user The user it is sent from.
     * @param ircMessage The message.
     */
    public void receive(IrcConnection server, User user, IrcMessage ircMessage) {
        try {
            userTabs.get(user).receive(ircMessage);
        } catch(NullPointerException npe) {
            Log.i(TAG, "Received message from user: " + user.getNick());
            createTab(server, user);
            userTabs.get(user).receive(ircMessage);
        }
    }

    public void handleError(IrcConnection ircConnection, Exception e) {
        Log.e(TAG, "ERROR: " + e.toString());
        e.printStackTrace();
        serverTabs.get(ircConnection).receive(new IrcMessage(
                ResCache.str(R.string.error),
                e.toString(),
                IrcMessage.Type.FATAL
        ));
    }


    public void updateMentionPattern(IrcConnection irc) {
        //Stole these from nirc; https://github.com/cjstewart88/nirc/blob/master/public/javascripts/client.js
        mentionParsers.put(irc, Pattern.compile("(^|[^a-zA-Z0-9\\[\\]{}\\^`|])" + irc.getClient().getNick() + "([^a-zA-Z0-9\\[\\]{}\\^`|]|$)"));
    }

    public void detectMention(IrcConnection irc, IrcMessage msg, String tabTag) {
        if(msg.getMessage()==null || !mentionParsers.get(irc).matcher(msg.getMessage()).find())
            return;
        int pos = msg.getMessage().indexOf(irc.getClient().getNick());
        if(pos==-1) //safety net
            return;
        IrcService.instance.handleMention(msg, tabTag);
    }

    /** Selects a tab that has been specified from a bundle (e.g. a notification) where passing Objects is impossible.
     *
     * NOTE: DOES NOT HANDLE SERVER. i.e. there could be two channels on two different servers with the same name and it could
     * bring up the wrong one.
     *
     * @param tabTag the same Tag used for the AB Tab. e.g. <serverAddress>:<channelName>
     */
    public void focusTab(String tabTag) {

        ActionBar abar = master.getSupportActionBar();
        for(int i=0; i<abar.getTabCount(); ++i)
            if(abar.getTabAt(i).getTag().equals(tabTag)) {
                abar.getTabAt(i).select();
                return;
            }

        Log.w(TAG, "Unable to find TabFragment: " + tabTag);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////// Classes ///////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    /** Listener for when the Tab changes */
    class ChatTabListener implements ActionBar.TabListener {
        TabFragment mFragment;
        public ChatTabListener(TabFragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.replace(R.id.main_fragment, mFragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            //ft.remove(mFragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
