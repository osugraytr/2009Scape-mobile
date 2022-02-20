package net.kdt.pojavlaunch.utils;

import net.kdt.pojavlaunch.sound.JAudioManager;
import net.kdt.pojavlaunch.JavaGUILauncherActivity;

public class JMessageHandler {
    public static void handleMessage(String message){
        if(!JavaGUILauncherActivity.isFocused)
            return;
        String[] args = message.split(" ");
        String command = args[1].replace(":","");
        switch(command){
            case "NEW_MUSIC":
                // EXAMPLE:    AMESSAGE NEW_MUSIC MUSIC_ID: 0 MUSIC_VOLUME: 121
                JAudioManager.setMusicVolume(Integer.parseInt(args[5])/255f);
                JAudioManager.setMusicTrack(Integer.parseInt(args[3]));
                break;
            case "NEW_EFFECT":
                // EXAMPLE:    AMESSAGE NEW_EFFECT EFFECT_ID: 2555 EFFECT_VOLUME: 10 EFFECT_DELAY: 0
                JAudioManager.setEffectVolume(Float.parseFloat(args[5])/10f);
                JAudioManager.setEffectTrack(Integer.parseInt(args[3]));
                break;
            case "MUSIC_VOLUME":
                // EXAMPLE:     AMESSAGE MUSIC_VOLUME: 107
                JAudioManager.setMusicVolume(Integer.parseInt(args[2])/255f);
                break;
            case "EFFECT_VOLUME":
                // EXAMPLE:     AMESSAGE EFFECT_VOLUME: 10
                JAudioManager.setEffectVolume(Float.parseFloat(args[2])/10f);
                break;
        }
    }
}
