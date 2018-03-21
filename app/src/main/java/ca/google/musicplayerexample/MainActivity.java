package ca.google.musicplayerexample;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity implements MediaController.MediaPlayerControl {

    private ArrayList<Song> songList; // ArrayList to hold all discovered music on the device
    private ListView songView; // ListView to display all the songs

    private MusicService musicService; // Represents the custom class we created
    private Intent playIntent; // The intent to play music within the MusicService class
    private boolean musicBound = false; // Flag to check if MainActivity is bound to MusicService

    private MusicController controller;

    private boolean paused, playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Checks for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                return;
            }
        }

        songView = findViewById(R.id.song_list);

        // Instantiate the song ArrayList
        songList = new ArrayList<Song>();

        // Get all songs on the device
        getSongList();

        // Sorts the songList alphabetically
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        // Creates a new adapter (using our custom class)
        // and sets it on the ListView
        SongAdapter songAdapter = new SongAdapter(this, songList);
        songView.setAdapter(songAdapter);

        setController();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If the intent doesn't exist, create one, bind to it, and start it
        if(playIntent == null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused = false;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            // Gets the reference to the service so we can interact with it
            musicService = binder.getService();
            // Passes the songList
            musicService.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    // When a song is selected
    public void songPicked(View view){
        musicService.setSong(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();

        if(playbackPaused){
            setController();
            playbackPaused = false;
        }

        controller.show(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                // Call the class that was created in MusicService.java
                musicService.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicService =null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // Method to retrieve song metadata
    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        // If there's music stored on the device
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //Gets song information
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);

            // Adds song to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    // Sets the controller up
    private void setController(){
        controller = new MusicController(this);

        // Setting the EventListeners for the 'previous song' and 'next song' buttons
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    // Plays the next song
    private void playNext(){
        // Calls the method that were made in MusicService.java
        musicService.playNext();

        if(playbackPaused){
            setController();
            playbackPaused = false;
        }

        controller.show(0);
    }

    // Plays the previous song
    private void playPrevious(){
        // Calls the method that were made in MusicService.java
        musicService.playPrevious();

        if(playbackPaused){
            setController();
            playbackPaused = false;
        }

        controller.show(0);
    }

    @Override
    public void start() {
        musicService.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        // Return the current song length if music is playing, else return 0
        if((musicService != null) && musicBound && musicService.isPng()) {
            return musicService.getDur();
        } else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        // Return the current position if music is playing, else return 0
        if((musicService != null) && musicBound && musicService.isPng()) {
            return musicService.getPosn();
        } else {
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        // Checks for certain parameters before checking if a song is playing
        if(musicService != null && musicBound) {
            return musicService.isPng();
        } else {
            return false;
        }
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}

// In-depth tutorial at => https://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764 (loading/displaying all audio files on device)
//          second part => https://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778 (music playback)
//           third part => https://code.tutsplus.com/tutorials/create-a-music-player-on-android-user-controls--mobile-22787 (refined music actions, music controls, and notification controls)