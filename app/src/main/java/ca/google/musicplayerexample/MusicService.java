package ca.google.musicplayerexample;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

// Handles the music playback
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    // Instance variables
    private MediaPlayer player; // Media player
    private ArrayList<Song> songList; // List of songList
    private int songIndex; // Song index/position
    private final IBinder musicBind = new MusicBinder();

    private String songTitle = ";";
    private String songArtist = ";";
    private static final int NOTIFY_ID = 1;

    private boolean shuffle = false;
    private Random rand;

    // TODO: set up AudioManager to handle audio focus between multiple sources

    // Occurs when the MusicService is created
    public void onCreate(){
        super.onCreate(); // Creates the service
        songIndex = 0; // Initialize position
        player = new MediaPlayer(); // Create the player
        rand = new Random(); // Instantiates the random variable for music shuffling

        initMusicPlayer();
    }

    // Initialize the music player
    public void initMusicPlayer(){
        // Configuring music player properties
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK); //PARTIAL_WAKE_LOCK allows music playback to continue when the device goes to sleep
        player.setAudioStreamType(AudioManager.STREAM_MUSIC); // Sets the stream type

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        // Release resources when the service instance is unbound (when the user exits the app)
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO: improve this area for later
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start music playback
        mp.start();

        // Setting up the notification that displays when a song plays
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle(songTitle)
                .setContentText(songArtist);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    // Receives the song list from the MainActivity
    public void setList(ArrayList<Song> songs){
        songList = songs;
    }

    // Selects a song
    public void setSong(int songIndex){
        this.songIndex = songIndex;
    }

    // Sets up a binding instance
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        // Resets the media player
        player.reset();

        // Gets the song
        Song currentSong = songList.get(songIndex);
        // Gets the song title
        songTitle = currentSong.getTitle();
        // Gets the song artist
        songArtist = currentSong.getArtist();
        // Gets the ID of the song
        long currSong = currentSong.getID();
        // Sets the Uri to the chosen song
        Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);

        // Attempting to set the data source of the media player to the song
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        // Uses an asynchronous method to play the song
        player.prepareAsync();
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int position){
        player.seekTo(position);
    }

    public void go(){
        player.start();
    }

    // Goes to previous song
    public void playPrevious(){
        // If 'previous song' is pressed on the first song, go to the last song
        songIndex--;
        if(songIndex < 0) {
            songIndex = songList.size() - 1;
        }

        playSong();
    }

    //skip to next song
    public void playNext(){
        if(shuffle && songList.size() > 1){
            // TODO: code proper shuffle function so a song isn't replayed until the rest have been played
            // Sets the next song to a random song that isn't the current song
            int newSong = songIndex;
            while((newSong == songIndex)){
                newSong=rand.nextInt(songList.size());
            }
            songIndex = newSong;
        } else {
            // If 'next song' is pressed on the last song, go to the first song
            songIndex++;
            if(songIndex >= songList.size()) {
                songIndex = 0;
            }
        }

        playSong();
    }

    public void setShuffle(){
        // Toggles the shuffle setting
        shuffle = !shuffle;
    }

    @Override
    public void onDestroy() {
        // Stopping the notification
        stopForeground(true);
    }

}
