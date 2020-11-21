// IAudioPlayerService.aidl
package com.example.mobileplayer;

// Declare any non-default types here with import statements

interface IAudioPlayerService {
    int getAudioSessionId();
    void start();
    void pause();
    void next();
    void prev();
    boolean isPlaying();
    void setPlayMode(int mode);
    long getCurrentPosition();
    long getDuration();
    String getArtist();
    String getName();
    void requireRefreshAudioInfoDisplay();
    void setPosition(int position);
    int getItemIndex();
    void setItemIndex(int currentIndex);
    void prepare();
}