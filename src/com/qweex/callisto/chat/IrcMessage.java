package com.qweex.callisto.chat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import com.qweex.callisto.R;
import com.qweex.callisto.ResCache;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class IrcMessage
{
    static SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");

    // Note that Type is ONLY FOR COLORING. All other typing and whatnot should be handled by sirc. That's why I chose it.
    public enum Type {  ACTION,
                        CONNECTION,
                        JOIN,
                        KICK,
                        MESSAGE,
                        MOTD,
                        MODE,
                        NOTICE,
                        PART,
                        TOPIC
                     }

    public enum UserMode {FOUNDER, ADMIN, OP, HALFOP, VOICE}

    CharSequence title, message;
    Calendar timestamp;
    Type type;


    public IrcMessage(String title, String message, Type type) {
        this(title, message, type, null);
    }

    public IrcMessage(String title, String message, Type type, Date time)
    {
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = Calendar.getInstance();
        if(time!=null)
            this.timestamp.setTime(time);
    }

    public Date getRawDate() {
        return timestamp.getTime();
    }

    public CharSequence getTime() {
        return sdfTime.format(timestamp.getTime());
    }

    public CharSequence getTitle() {
        return title;
    }

    public CharSequence getMessage() {
        return message;
    }

    public int getTimeColor() {
        return ResCache.clr(R.color.chat_time);
    }

    public int getTitleColor() {
        switch(type) {
            case MESSAGE:
                //if(title==me)
                    //return ResCache.clr(R.color.chat_nick_me);
                return ResCache.clr(R.color.chat_nick);
            default:
                return getMessageColor();
        }
    }

    public int getMessageColor() {
        switch(type) {
            case ACTION:
                return ResCache.clr(R.color.chat_action);
            case CONNECTION:
                return ResCache.clr(R.color.chat_connection);
            case JOIN:
                return ResCache.clr(R.color.chat_join);
            case KICK:
                return ResCache.clr(R.color.chat_kick);
            case MOTD:
                return ResCache.clr(R.color.chat_motd);
            case MODE:
                return ResCache.clr(R.color.chat_mode);
            case NOTICE:
                return ResCache.clr(R.color.chat_notice);
            case PART:
                return ResCache.clr(R.color.chat_part);
            case TOPIC:
                return ResCache.clr(R.color.chat_topic);

            case MESSAGE:
            default:
                return ResCache.clr(R.color.chat_text);
        }
    }


    public ColorStateList getLinkColors() {
        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed},
                        new int[] { android.R.attr.state_focused},
                        new int[] {}
                },
                new int [] {
                        Color.BLUE,
                        0xFF000000 + ResCache.clr(R.color.chat_link),
                        0xFF000000 + ResCache.clr(R.color.chat_link),
                }
        );
    }

    @Override
    public String toString() {
        return getTime() +
                (getTitle()!=null ? (" " + getTitle()) : "") +
                (getMessage()!=null ? (" " + getMessage()) : "");
    }
}