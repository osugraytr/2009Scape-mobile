package net.kdt.pojavlaunch.sound;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import net.kdt.pojavlaunch.utils.Tools;

import java.io.File;
import java.io.IOException;

public class SoundService extends Service {

    private static MediaPlayer musicPlayer = new MediaPlayer();
    private static MediaPlayer sfxPlayer = new MediaPlayer();
    private static final String TAG = "SoundService";
    private static Uri musicTrack;
    private static Uri sfxTrack;
    private static float musicVolume = -1f;
    private static float sfxVolume = -1f;

    public static void setMusicTrack(int t) {
        musicTrack = Uri.fromFile(new File(Tools.DIR_DATA + "/music/" + t + ".ogg"));
        resetMusicAndPlay();
    }

    public static void setMusicVolume(float v) {
        musicVolume = v;
        if(isPlayingMusic()){
            musicPlayer.setVolume(v,v);
        }
    }

    public static void resetMusicAndPlay() {
        musicPlayer.reset();
        try {
            musicPlayer.setDataSource(musicTrack.getPath());
            musicPlayer.setVolume(musicVolume, musicVolume);
            musicPlayer.prepare();
            musicPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPlayingMusic(){
        try{
            return musicPlayer.isPlaying();
        } catch (IllegalStateException e){
            System.out.println("musicPlayer was released. Resetting.");
        }
        musicPlayer = new MediaPlayer();
        return false;
    }

    public static boolean isPlayingSFX(){
        try{
            return sfxPlayer.isPlaying();
        } catch (IllegalStateException e){
            System.out.println("musicPlayer was released. Resetting.");
        }
        sfxPlayer = new MediaPlayer();
        return false;
    }

    public static void setSFXTrack(int t) {
        sfxTrack = Uri.fromFile(new File(Tools.DIR_DATA + "/effects/" + t + ".ogg"));
        resetSFXAndPlay();
    }

    public static void setSFXVolume(float v) {
        sfxVolume = v;
        if(isPlayingSFX()){
            sfxPlayer.setVolume(v,v);
        }
    }

    public static void resetSFXAndPlay() {
        sfxPlayer.reset();
        try {
            sfxPlayer.setDataSource(sfxTrack.getPath());
            sfxPlayer.setVolume(sfxVolume, sfxVolume);
            sfxPlayer.prepare();
            sfxPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBind()" );
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        musicPlayer.stop();
        musicPlayer.release();
        sfxPlayer.stop();
        sfxPlayer.release();
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory()");
    }
}