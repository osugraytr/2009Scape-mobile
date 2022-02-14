package net.kdt.pojavlaunch;

import android.content.Intent;

import net.kdt.SoundService;

public class JAudioManager {

    static float savedMusicVolume = 0;
    static float savedEffectVolume = 0;

    public static void init(){
        Intent soundService = new Intent(JavaGUILauncherActivity.getContext(), SoundService.class);
        JavaGUILauncherActivity.getContext().startService(soundService);
    }

    public static void setMusicTrack(int track) {
        SoundService.setMusicTrack(track);
        SoundService.resetMusicAndPlay();
    }

    public static void setMusicVolume(float volume){
        savedMusicVolume = volume;
        SoundService.setMusicVolume(volume);
    }

    public static void setEffectVolume(float volume){
        savedEffectVolume = volume;
        SoundService.setSFXVolume(savedEffectVolume);
    }

    public static void setEffectTrack(int track) {
        SoundService.setSFXTrack(track);
        SoundService.resetSFXAndPlay();
    }

    public static void muteSound(){
        if (SoundService.isPlayingMusic())
            SoundService.setMusicVolume(0);
        if (SoundService.isPlayingSFX())
            SoundService.setSFXVolume(0);
    }

    public static void resumeSound(){
        if (SoundService.isPlayingMusic())
            setMusicVolume(savedMusicVolume);
        if (SoundService.isPlayingSFX())
            setEffectVolume(savedEffectVolume);
    }

}
